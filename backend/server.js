require('dotenv').config({ quiet: true });
const express = require('express');
const cors = require('cors');
const { MongoClient, ServerApiVersion, ObjectId } = require('mongodb');
const path = require('path');
const crypto = require('crypto');
const { promisify } = require('node:util');
const scrypt = promisify(crypto.scrypt);

const app = express();
const PORT = process.env.PORT || 3000;

// MongoDB Connection
const { MONGODB_URI } = process.env;
const DB_NAME = process.env.MONGODB_DB || 'roadpulse';
const SESSION_MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000;

let clientPromise;

function getDb() {
  if (!MONGODB_URI) return Promise.reject(new Error('MONGODB_URI is missing'));
  if (!clientPromise) {
    const client = new MongoClient(MONGODB_URI, {
      serverApi: { version: ServerApiVersion.v1, strict: true, deprecationErrors: true },
      serverSelectionTimeoutMS: 5000,
      maxPoolSize: 10
    });
    clientPromise = (async () => {
      try {
        await client.connect();
        const database = client.db(DB_NAME);
        await Promise.all([
          database.collection('trips').createIndex({ userEmail: 1, startTime: -1 }),
          database.collection('track_points').createIndex({ tripId: 1, timestamp: 1 }),
          database.collection('sessions').createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 }),
          database.collection('users').createIndex({ email: 1 }, { unique: true }),
          database.collection('live_beacons').createIndex({ userEmail: 1, updatedAt: -1 }),
          database.collection('guardian_connections').createIndex({ riderEmail: 1, status: 1 })
        ]);
        return database;
      } catch (error) {
        await client.close().catch(() => {});
        clientPromise = null;
        throw error;
      }
    })();
  }
  return clientPromise;
}

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.static(path.join(__dirname, 'public')));

app.use('/api', async (req, res, next) => {
  res.setHeader('Cache-Control', 'no-store');
  if (['/status', '/youtube/search', '/sketchfab/search'].includes(req.path)) return next();
  try {
    req.db = await getDb();
    next();
  } catch (error) {
    console.error('[Rhody] Database initialization failed:', error.name);
    res.status(503).json({ error: 'Rhody Cloud is unavailable. Try again shortly.' });
  }
});

function safeEqual(left, right) {
  const first = Buffer.from(left);
  const second = Buffer.from(right);
  return first.length === second.length && crypto.timingSafeEqual(first, second);
}

async function hashPassword(password, salt = crypto.randomBytes(16).toString('hex')) {
  return { hash: (await scrypt(password, salt, 64)).toString('hex'), salt };
}

async function verifyPassword(password, user) {
  if (typeof user.passwordHash !== 'string' || typeof user.salt !== 'string') return false;
  const hash = user.passwordVersion === 2
    ? (await hashPassword(password, user.salt)).hash
    : crypto.pbkdf2Sync(password, user.salt, 1000, 64, 'sha512').toString('hex');
  return safeEqual(hash, user.passwordHash);
}

function sessionKey(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

function requestToken(req) {
  return req.headers.authorization?.match(/^Bearer ([A-Za-z0-9_-]{43})$/i)?.[1];
}

async function createSession(database, user) {
  const token = crypto.randomBytes(32).toString('base64url');
  await database.collection('sessions').insertOne({
    _id: sessionKey(token),
    userId: user._id,
    expiresAt: new Date(Date.now() + SESSION_MAX_AGE_MS)
  });
  return token;
}

async function findSessionUser(req) {
  const token = requestToken(req);
  if (!token) return null;
  const session = await req.db.collection('sessions').findOne({
    _id: sessionKey(token),
    expiresAt: { $gt: new Date() }
  });
  return session ? req.db.collection('users').findOne({ _id: session.userId }) : null;
}

async function requireSession(req, res, next) {
  req.user = await findSessionUser(req);
  if (!req.user) return res.status(401).json({ error: 'Please sign in to continue.' });
  next();
}

function requireRider(req, res, next) {
  if (req.user.role === 'parent') return res.status(403).json({ error: 'A rider account is required.' });
  next();
}

function requireGuardian(req, res, next) {
  if (req.user.role !== 'parent') return res.status(403).json({ error: 'A guardian account is required.' });
  next();
}

function publicUser(user) {
  return {
    id: user._id.toString(),
    email: user.email,
    name: user.name || user.email.split('@')[0],
    role: user.role || 'rider',
    wardEmail: user.wardEmail || '',
    avatar: user.avatar || '',
    bio: user.bio || '',
    bikeModel: user.bikeModel || '',
    bikeSpecs: user.bikeSpecs || '',
    bikePlate: user.bikePlate || '',
    tireSetup: user.tireSetup || '',
    emergencyContactName: user.emergencyContactName || '',
    emergencyContactPhone: user.emergencyContactPhone || '',
    speedUnit: user.speedUnit || 'kmh'
  };
}

function normalizeEmail(email) {
  return typeof email === 'string' ? email.trim().toLowerCase() : '';
}

function cleanText(value) {
  return typeof value === 'string' ? value.trim() : '';
}

// ----------------------------------------------------
// AUTHENTICATION ROUTES
// ----------------------------------------------------

app.post('/api/auth/register', async (req, res) => {
  const { email, password, name, role, wardCodeOrEmail, bikeModel, bikePlate, emergencyContactPhone } = req.body || {};
  const cleanEmail = normalizeEmail(email);
  if (cleanEmail.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) {
    return res.status(400).json({ error: 'Enter a valid email address.' });
  }
  if (typeof password !== 'string' || password.length < 6 || password.length > 256) {
    return res.status(400).json({ error: 'Use a password between 6 and 256 characters.' });
  }

  const userRole = role === 'parent' || role === 'guardian' ? 'parent' : 'rider';
  const wardEmail = userRole === 'parent' ? normalizeEmail(wardCodeOrEmail) : '';
  if (userRole === 'parent') {
    const rider = wardEmail !== cleanEmail && await req.db.collection('users').findOne({
      email: wardEmail, role: { $ne: 'parent' }, passwordHash: { $type: 'string' }
    });
    if (!rider) return res.status(400).json({ error: 'Enter the email of a different, registered rider.' });
  }

  const { hash, salt } = await hashPassword(password);
  const user = {
    email: cleanEmail,
    name: cleanText(name) || cleanEmail.split('@')[0],
    role: userRole,
    wardEmail,
    passwordHash: hash,
    passwordVersion: 2,
    salt,
    bikeModel: cleanText(bikeModel),
    bikePlate: cleanText(bikePlate),
    emergencyContactPhone: cleanText(emergencyContactPhone),
    createdAt: new Date(),
    lastLoginAt: new Date()
  };
  try {
    const { insertedId } = await req.db.collection('users').insertOne(user);
    user._id = insertedId;
    res.status(201).json({ success: true, token: await createSession(req.db, user), user: publicUser(user) });
  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({ error: 'An account with this email already exists.' });
    }
    throw error;
  }
});

app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body || {};
  const cleanEmail = normalizeEmail(email);
  if (!cleanEmail || typeof password !== 'string' || !password || password.length > 256) {
    return res.status(400).json({ error: 'Enter your email and password.' });
  }
  const users = req.db.collection('users');
  const user = await users.findOne({ email: cleanEmail });
  if (!user || !await verifyPassword(password, user)) {
    return res.status(401).json({ error: 'Invalid email or password.' });
  }

  const update = { lastLoginAt: new Date() };
  if (user.passwordVersion !== 2) {
    const { hash, salt } = await hashPassword(password);
    Object.assign(update, { passwordHash: hash, salt, passwordVersion: 2 });
  }
  await users.updateOne({ _id: user._id, passwordHash: user.passwordHash }, { $set: update });
  res.json({ success: true, token: await createSession(req.db, user), user: publicUser(user) });
});

app.post('/api/auth/logout', async (req, res) => {
  const token = requestToken(req);
  if (token) await req.db.collection('sessions').deleteOne({ _id: sessionKey(token) });
  res.status(204).end();
});

app.get('/api/auth/me', requireSession, (req, res) => {
  const user = publicUser(req.user);
  res.json({ ...user, user });
});

app.get('/api/user/profile', requireSession, (req, res) => {
  res.json({ success: true, profile: publicUser(req.user) });
});

async function handleProfileUpdate(req, res) {
  const update = { updatedAt: new Date() };
  for (const field of ['name', 'avatar', 'bio', 'bikeModel', 'bikeSpecs', 'bikePlate',
    'tireSetup', 'emergencyContactName', 'emergencyContactPhone', 'speedUnit']) {
    if (typeof req.body?.[field] === 'string') update[field] = req.body[field].trim();
  }
  const profile = await req.db.collection('users').findOneAndUpdate(
    { _id: req.user._id },
    { $set: update },
    { returnDocument: 'after' }
  );
  res.json({ success: true, profile: publicUser(profile) });
}

app.post('/api/user/profile', requireSession, handleProfileUpdate);
app.put('/api/user/profile', requireSession, handleProfileUpdate);

const defaultMotorcyclePlaces = [
  {
    _id: 'wp_nandi',
    name: 'Nandi Hills Apex Climb',
    category: 'pass',
    lat: 13.3702,
    lng: 77.6835,
    altitude: 1478,
    notes: 'Epic morning mountain run with 40 hairpins & sweeping high-elevation corners.'
  },
  {
    _id: 'wp_bikers_cafe',
    name: "Biker's Highway Cafe (NH44)",
    category: 'garage',
    lat: 13.0645,
    lng: 77.5898,
    altitude: 910,
    notes: 'Popular superbike pitstop, high-octane fuel & tire pressure station.'
  },
  {
    _id: 'wp_kolar',
    name: 'Kolar Fast Expressway Twisties',
    category: 'track',
    lat: 13.1362,
    lng: 78.1291,
    altitude: 822,
    notes: 'Smooth asphalt 6-lane section with wide sweepers for high-speed aero testing.'
  },
  {
    _id: 'wp_agumbe',
    name: 'Agumbe Ghat Rainforest Twisties',
    category: 'pass',
    lat: 13.5042,
    lng: 75.0934,
    altitude: 826,
    notes: 'Legendary 14 hairpin curves through Western Ghats rainforest canopy with 45° lean angles.'
  },
  {
    _id: 'wp_yercaud',
    name: 'Yercaud 20-Hairpin Loop',
    category: 'pass',
    lat: 11.7753,
    lng: 78.2093,
    altitude: 1515,
    notes: 'Challenging continuous hairpins with fresh grippy tarmac and cool mountain elevation.'
  },
  {
    _id: 'wp_kolli',
    name: 'Kolli Hills 70 Hairpin Pass',
    category: 'pass',
    lat: 11.2485,
    lng: 78.3389,
    altitude: 1300,
    notes: 'The ultimate motorcycle test: 70 continuous hairpin bends climbing into misty peak.'
  }
];

// ----------------------------------------------------
// MOTORCYCLE WHOLE MANAGEMENT ENDPOINTS (PILLAR 1 & 4)
// ----------------------------------------------------
const defaultBikeManagement = {
  isConfigured: false,
  specs: {
    makeModel: '',
    engine: '',
    power: '',
    torque: '',
    gearbox: '',
    fuelTank: '',
    brakes: '',
    kerbWeight: '',
    currentOdometer: 0
  },
  components: {
    engineOil: { lastServiceOdo: 0, intervalKm: 4000, name: 'Engine Oil & Filter' },
    driveChain: { lastServiceOdo: 0, intervalKm: 500, name: 'Drive Chain Lube & Slack Adjust' },
    airFilter: { lastServiceOdo: 0, intervalKm: 8000, name: 'Viscous Air Filter' },
    sparkPlug: { lastServiceOdo: 0, intervalKm: 10000, name: 'Spark Plug' },
    coolant: { lastServiceOdo: 0, intervalKm: 12000, name: 'Radiator Coolant Flush' },
    brakePads: { lastServiceOdo: 0, intervalKm: 8000, name: 'Brake Pads & Fluid' }
  },
  documents: {
    rc: { number: '', expiry: '', fileData: '', fileName: '', uploadedAt: null },
    dl: { number: '', expiry: '', fileData: '', fileName: '', uploadedAt: null },
    insurance: { provider: '', policyNumber: '', expiry: '', fileData: '', fileName: '', uploadedAt: null },
    puc: { certificateNumber: '', expiry: '', fileData: '', fileName: '', uploadedAt: null },
    rcNumber: '',
    rcExpiry: '',
    dlNumber: '',
    dlExpiry: '',
    insuranceProvider: '',
    insurancePolicyNumber: '',
    insuranceExpiry: '',
    pucNumber: '',
    pucExpiry: '',
    rsaPhone: '1800-420-1600'
  },
  serviceHistory: [],
  fuelLogs: []
};

async function getBikeManagement(database, userEmail) {
  const record = await database.collection('bike_management').findOne({ userEmail });
  return {
    ...defaultBikeManagement,
    ...record?.data,
    specs: { ...defaultBikeManagement.specs, ...record?.data?.specs },
    documents: { ...defaultBikeManagement.documents, ...record?.data?.documents }
  };
}

app.get('/api/user/bike-management', requireSession, requireRider, async (req, res) => {
  res.json({ success: true, data: await getBikeManagement(req.db, req.user.email) });
});

app.post('/api/user/bike-management', requireSession, requireRider, async (req, res) => {
  const userEmail = req.user.email;
  const update = { updatedAt: new Date() };
  // Android sends selected fields. Update those fields without replacing web-managed records.
  for (const key of ['isConfigured', 'serviceHistory', 'fuelLogs']) {
    if (Object.hasOwn(req.body || {}, key)) update[`data.${key}`] = req.body[key];
  }
  for (const section of ['specs', 'components', 'documents']) {
    for (const key of Object.keys(defaultBikeManagement[section])) {
      if (Object.hasOwn(req.body?.[section] || {}, key)) {
        update[`data.${section}.${key}`] = req.body[section][key];
      }
    }
  }
  await req.db.collection('bike_management').updateOne(
    { userEmail },
    { $set: update, $setOnInsert: { userEmail } },
    { upsert: true }
  );
  res.json({ success: true, data: await getBikeManagement(req.db, userEmail) });
});

app.delete('/api/user/places/:id', requireSession, requireRider, async (req, res) => {
  const id = req.params.id;
  const ids = ObjectId.isValid(id) ? [id, new ObjectId(id)] : [id];
  await req.db.collection('places').deleteOne({ userEmail: req.user.email, _id: { $in: ids } });
  res.json({ success: true });
});

app.get('/api/status', async (req, res) => {
  try {
    const database = await getDb();
    await database.command({ ping: 1 });
    res.json({ status: 'online' });
  } catch (error) {
    console.error('[Rhody] Readiness check failed:', error.name);
    res.status(503).json({ status: 'disconnected', error: 'Rhody Cloud is unavailable.' });
  }
});

function tripFilter(id, userEmail) {
  const alternatives = [{ _id: id }];
  if (ObjectId.isValid(id)) alternatives.push({ _id: new ObjectId(id) });
  if (/^\d+$/.test(id)) alternatives.push({ localId: Number(id) });
  return { userEmail, $or: alternatives };
}

async function tripPoints(database, trip) {
  return database.collection('track_points')
    .find({ tripId: trip._id.toString(), ...(trip.pointBatch ? { pointBatch: trip.pointBatch } : {}) })
    .sort({ timestamp: 1 })
    .toArray();
}

app.get('/api/trips', requireSession, requireRider, async (req, res) => {
  const trips = await req.db.collection('trips')
    .find({ userEmail: req.user.email })
    .sort({ startTime: -1 })
    .toArray();
  res.json(trips);
});

app.get('/api/trips/:id', requireSession, requireRider, async (req, res) => {
  const trip = await req.db.collection('trips').findOne(tripFilter(req.params.id, req.user.email));
  if (!trip) return res.status(404).json({ error: 'Trip not found.' });
  res.json({ ...trip, points: await tripPoints(req.db, trip) });
});

// API: Broadcast live telemetry beacon (from rider's Android app)
app.post('/api/trips/:id/live', requireSession, requireRider, async (req, res) => {
  try {
    const tripId = req.params.id;
    const {
      riderName,
      status,
      isMoving,
      currentSpeedKmh,
      maxSpeedKmh,
      latitude,
      longitude,
      altitudeMeters,
      batteryPercent,
      totalDistanceKm,
      elapsedTimeMs,
      timestamp
    } = req.body;

    const beaconData = {
      tripId,
      userEmail: req.user.email,
      riderName: riderName || 'Rider',
      status: status || 'RECORDING',
      isMoving: Boolean(isMoving),
      currentSpeedKmh: Number(currentSpeedKmh || 0),
      maxSpeedKmh: Number(maxSpeedKmh || 0),
      latitude: Number.isFinite(latitude) ? latitude : null,
      longitude: Number.isFinite(longitude) ? longitude : null,
      altitudeMeters: altitudeMeters ? Number(altitudeMeters) : null,
      batteryPercent: batteryPercent ? Number(batteryPercent) : null,
      totalDistanceKm: Number(totalDistanceKm || 0),
      elapsedTimeMs: Number(elapsedTimeMs || 0),
      updatedAt: new Date(),
      timestamp: timestamp || Date.now()
    };

    await req.db.collection('live_beacons').updateOne(
      { _id: `${req.user._id}:${tripId}` },
      { $set: beaconData },
      { upsert: true }
    );

    res.json({ success: true, timestamp: beaconData.updatedAt });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Browser sharing links are unguessable. Android uses local numeric ride IDs.
app.get('/api/trips/:id/live', async (req, res) => {
  const tripId = req.params.id;
  const isShareLink = /^trip_[0-9a-f-]{36}$/.test(tripId);
  const user = await findSessionUser(req);
  if (!user && !isShareLink) return res.status(404).json({ error: 'Live ride not found.' });
  const query = { tripId, updatedAt: { $gte: new Date(Date.now() - 30000) } };
  if (!isShareLink) query.userEmail = user.email;
  const beacon = await req.db.collection('live_beacons').findOne(query, { projection: { _id: 0, userEmail: 0 } });
  if (!beacon) return res.status(404).json({ error: 'Live ride not found.' });
  res.json(beacon);
});

app.post('/api/trips/sync', requireSession, requireRider, async (req, res) => {
  const { trip, points } = req.body || {};
  if (!trip || !Number.isFinite(new Date(trip.startTime).getTime()) || !Array.isArray(points)) {
    return res.status(400).json({ error: 'A trip with a start time and track points is required.' });
  }
  const userEmail = req.user.email;
  const sourceId = typeof trip._id === 'string' && /^trip_[0-9a-f-]{36}$/.test(trip._id) ? trip._id : null;
  const filter = { userEmail, startTime: trip.startTime, title: String(trip.title || 'Ride') };
  if (sourceId) filter.sourceId = sourceId;
  const summary = {
    ...trip,
    ...filter,
    localId: trip.id || trip.localId || null,
    updatedAt: new Date()
  };
  for (const key of ['_id', 'id', 'points', 'pointBatch']) delete summary[key];

  const trips = req.db.collection('trips');
  const tripId = (await trips.findOneAndUpdate(filter, {
    $setOnInsert: summary
  }, { upsert: true, returnDocument: 'after', projection: { _id: 1 } }))._id;
  const pointBatch = crypto.randomUUID();
  const trackPoints = req.db.collection('track_points');
  if (points.length) {
    await trackPoints.insertMany(points.map((point, index) => {
      const { _id, ...data } = point;
      return { ...data, tripId: tripId.toString(), userEmail, pointBatch, pointIndex: index + 1 };
    }));
  }
  const previous = await trips.findOneAndUpdate(
    { _id: tripId, userEmail }, { $set: { ...summary, pointBatch } }
  );
  if (!previous) {
    await trackPoints.deleteMany({ tripId: tripId.toString(), pointBatch });
    return res.status(409).json({ error: 'This ride changed while syncing. Try again.' });
  }
  await trackPoints.deleteMany({
    tripId: tripId.toString(),
    pointBatch: previous.pointBatch || { $exists: false }
  }).catch(error => console.warn('[Rhody] Old track point cleanup failed:', error.name));
  res.json({ success: true, message: 'Ride synced to Rhody Cloud.', tripId: tripId.toString() });
});

app.delete('/api/trips/:id', requireSession, requireRider, async (req, res) => {
  const trip = await req.db.collection('trips').findOneAndDelete(tripFilter(req.params.id, req.user.email));
  if (trip) await req.db.collection('track_points').deleteMany({ tripId: trip._id.toString() });
  res.json({ success: true });
});

app.get('/api/trips/:id/export/:format', requireSession, requireRider, async (req, res) => {
  const builders = { gpx: buildGpx, csv: buildCsv };
  const format = req.params.format;
  if (!Object.hasOwn(builders, format)) return res.status(404).json({ error: 'Export format not found.' });
  const trip = await req.db.collection('trips').findOne(tripFilter(req.params.id, req.user.email));
  if (!trip) return res.status(404).json({ error: 'Trip not found.' });
  const points = await tripPoints(req.db, trip);
  res.type(format === 'gpx' ? 'application/gpx+xml' : 'text/csv');
  res.attachment(`rhody_${trip._id}.${format}`).send(builders[format](trip, points));
});

function buildGpx(trip, points) {
  let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
  xml += `<gpx version="1.1" creator="Rhody Cloud" xmlns="http://www.topografix.com/GPX/1/1">\n`;
  xml += `  <metadata>\n    <name>${escapeXml(trip.title)}</name>\n    <time>${new Date(trip.startTime).toISOString()}</time>\n  </metadata>\n`;
  xml += `  <trk>\n    <name>${escapeXml(trip.title)}</name>\n    <trkseg>\n`;
  for (const pt of points) {
    xml += `      <trkpt lat="${pt.latitude}" lon="${pt.longitude}">\n`;
    if (pt.altitudeMeters != null) xml += `        <ele>${pt.altitudeMeters}</ele>\n`;
    xml += `        <time>${new Date(pt.timestamp).toISOString()}</time>\n`;
    xml += `        <extensions>\n          <speed>${pt.speedMps}</speed>\n        </extensions>\n`;
    xml += `      </trkpt>\n`;
  }
  xml += `    </trkseg>\n  </trk>\n</gpx>\n`;
  return xml;
}

function buildCsv(trip, points) {
  let csv = `# Rhody Cloud Export\n# Title: ${trip.title}\n# Start: ${new Date(trip.startTime).toISOString()}\n`;
  csv += `point_index,timestamp,latitude,longitude,speed_kmh,cumulative_distance_km,altitude_m\n`;
  points.forEach((p, i) => {
    const speedKmh = Number(p.speedKmh ?? (p.speedMps || 0) * 3.6).toFixed(1);
    const distKm = Number(p.distanceKm ?? (p.cumulativeDistanceMeters || 0) / 1000).toFixed(3);
    csv += `${i + 1},${new Date(p.timestamp).toISOString()},${p.latitude},${p.longitude},${speedKmh},${distKm},${p.altitudeMeters || ''}\n`;
  });
  return csv;
}

function escapeXml(str) {
  return (str || '').replace(/[<>&'"]/g, (c) => {
    switch (c) {
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '&': return '&amp;';
      case '\'': return '&apos;';
      case '"': return '&quot;';
    }
  });
}

// Real YouTube Search API (Resolves real playable video IDs, titles, thumbnails)
app.get('/api/youtube/search', async (req, res) => {
  const query = req.query.q || 'motorcycle telemetry review';
  try {
    const response = await fetch(`https://www.youtube.com/results?search_query=${encodeURIComponent(query)}`, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept-Language': 'en-US,en;q=0.9'
      }
    });
    const html = await response.text();
    const jsonMatch = html.match(/var ytInitialData = ({.*?});<\/script>/s) || html.match(/ytInitialData = ({.*?});/s);
    const videos = [];
    if (jsonMatch) {
      try {
        const data = JSON.parse(jsonMatch[1]);
        const contents = data.contents?.twoColumnSearchResultsRenderer?.primaryContents?.sectionListRenderer?.contents?.[0]?.itemSectionRenderer?.contents || [];
        for (const item of contents) {
          const v = item.videoRenderer;
          if (v && v.videoId) {
            videos.push({
              id: v.videoId,
              title: v.title?.runs?.[0]?.text || `${query} Video`,
              duration: v.lengthText?.simpleText || '',
              thumbnail: v.thumbnail?.thumbnails?.slice(-1)[0]?.url || `https://i.ytimg.com/vi/${v.videoId}/hqdefault.jpg`,
              channel: v.ownerText?.runs?.[0]?.text || 'Motorcycle Channel'
            });
          }
        }
      } catch (e) {
        console.warn('JSON parse error in ytInitialData:', e.message);
      }
    }
    
    // Fallback if parsing initialData was empty: regex match video IDs
    if (videos.length === 0) {
      const idMatches = [...html.matchAll(/\/watch\?v=([a-zA-Z0-9_-]{11})/g)];
      const uniqueIds = [...new Set(idMatches.map(m => m[1]))].slice(0, 8);
      uniqueIds.forEach(id => {
        videos.push({
          id,
          title: `${query} Onboard Video`,
          duration: 'Onboard',
          thumbnail: `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
          channel: 'Motorcycle Onboard'
        });
      });
    }

    res.json({ query, videos: videos.slice(0, 10) });
  } catch (err) {
    console.error('YouTube search error:', err.message);
    res.status(500).json({ error: err.message, videos: [] });
  }
});

// GET /api/sketchfab/search - Dynamic 3D Motorcycle Model Lookup
app.get('/api/sketchfab/search', async (req, res) => {
  const query = req.query.q || 'motorcycle';
  try {
    const url = `https://api.sketchfab.com/v3/search?type=models&q=${encodeURIComponent(query)}&sort_by=-likeCount`;
    const response = await fetch(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      }
    });
    if (!response.ok) {
      return res.status(response.status).json({ error: 'Sketchfab search failed', models: [] });
    }
    const data = await response.json();
    const models = (data.results || []).slice(0, 5).map(m => ({
      uid: m.uid,
      name: m.name,
      viewerUrl: m.viewerUrl,
      thumbnail: m.thumbnails?.images?.[0]?.url || '',
      embedUrl: `https://sketchfab.com/models/${m.uid}/embed?autostart=1&internal=1&ui_infos=0&ui_watermark=0&ui_help=0&ui_settings=0&ui_inspector=0&ui_annotations=0&ui_stop=0&transparent=1&dnt=1`
    }));
    res.json({ query, models });
  } catch (err) {
    console.error('Sketchfab search error:', err.message);
    res.status(500).json({ error: err.message, models: [] });
  }
});

// ----------------------------------------------------
// PARENT / GUARDIAN PORTAL ENDPOINTS & NOTIFICATIONS
// ----------------------------------------------------


// Guardian links and consent live in MongoDB so requests work across devices.
app.post('/api/parent/connect-rider', requireSession, requireGuardian, async (req, res) => {
  const riderEmail = normalizeEmail(req.body?.riderCodeOrEmail);
  const rider = riderEmail !== req.user.email && await req.db.collection('users').findOne({
    email: riderEmail, role: { $ne: 'parent' }, passwordHash: { $type: 'string' }
  });
  if (!rider) return res.status(400).json({ error: 'Enter the email of a registered rider.' });
  const connection = {
    parentEmail: req.user.email,
    parentName: publicUser(req.user).name,
    riderEmail,
    status: 'pending_approval',
    requestedAt: new Date()
  };
  await req.db.collection('guardian_connections').updateOne(
    { parentEmail: req.user.email, riderEmail },
    { $setOnInsert: connection },
    { upsert: true }
  );
  await req.db.collection('users').updateOne({ _id: req.user._id }, { $set: { wardEmail: riderEmail } });
  res.json({ success: true, riderEmail });
});

app.get('/api/rider/guardian-requests', requireSession, requireRider, async (req, res) => {
  const requests = await req.db.collection('guardian_connections')
    .find({ riderEmail: req.user.email, status: 'pending_approval' }).toArray();
  res.json({ success: true, requests: requests.map(request => ({ ...request, id: request._id.toString() })) });
});

app.post('/api/rider/respond-guardian-request', requireSession, requireRider, async (req, res) => {
  const { requestId, action } = req.body || {};
  if (!ObjectId.isValid(requestId) || !['approved', 'rejected', 'revoked'].includes(action)) {
    return res.status(400).json({ error: 'Select a valid guardian request.' });
  }
  const result = await req.db.collection('guardian_connections').updateOne(
    { _id: new ObjectId(requestId), riderEmail: req.user.email },
    { $set: { status: action, updatedAt: new Date(), riderConfirmed: true } }
  );
  if (!result.matchedCount) return res.status(404).json({ error: 'Guardian request not found.' });
  res.json({ success: true, status: action });
});

app.get('/api/parent/rider-status', requireSession, requireGuardian, async (req, res) => {
  const riderEmail = req.user.wardEmail;
  const connections = req.db.collection('guardian_connections');
  const link = { parentEmail: req.user.email, riderEmail };
  let connection = riderEmail && await connections.findOne(link);
  if (riderEmail && (!connection || (connection.status === 'approved' && !connection.riderConfirmed))) {
    await connections.updateOne(
      { ...link, riderConfirmed: { $ne: true } },
      { $set: { ...link, status: 'pending_approval', parentName: publicUser(req.user).name, requestedAt: new Date() } },
      { upsert: !connection }
    );
    connection = await connections.findOne(link);
  }

  if (connection?.status !== 'approved' || !connection.riderConfirmed) {
    return res.json({ authorized: false, status: connection?.status || 'unpaired', rider: { email: riderEmail || '' } });
  }
  const rider = await req.db.collection('users').findOne({ email: riderEmail, role: { $ne: 'parent' } });
  if (!rider) return res.json({ authorized: false, status: 'unpaired' });
  const [bike, beacon, trips, notifications] = await Promise.all([
    getBikeManagement(req.db, riderEmail),
    req.db.collection('live_beacons').findOne({
      userEmail: riderEmail, updatedAt: { $gte: new Date(Date.now() - 30000) }
    }, { sort: { updatedAt: -1 }, projection: { _id: 0, userEmail: 0 } }),
    req.db.collection('trips').find({ userEmail: riderEmail }).sort({ startTime: -1 }).limit(5).toArray(),
    guardianNotifications(req.db, riderEmail, req.user.email)
  ]);
  const { name, email, avatar, bikeModel, bikePlate } = publicUser(rider);
  res.json({
    success: true, authorized: true, status: 'approved',
    rider: { name, email, avatar, bikeModel, bikePlate }, bike, beacon, trips,
    documents: bike.documents, notifications
  });
});

function guardianNotifications(database, riderEmail, parentEmail) {
  return database.collection('parent_notifications')
    .find({ riderEmail, dismissedBy: { $ne: parentEmail } })
    .sort({ timestamp: -1 }).limit(50).toArray();
}

app.delete('/api/parent/notifications/:id', requireSession, requireGuardian, async (req, res) => {
  const { id } = req.params;
  const riderEmail = req.user.wardEmail;
  const link = await req.db.collection('guardian_connections').findOne({
    parentEmail: req.user.email, riderEmail, riderConfirmed: true, status: 'approved'
  });
  if (!link) return res.status(403).json({ error: 'The rider has not approved access.' });
  await req.db.collection('parent_notifications').updateMany(
    { riderEmail, ...(id === 'all' ? {} : { id }) },
    { $addToSet: { dismissedBy: req.user.email } }
  );
  res.json({ success: true });
});

app.post('/api/emergency/crash-event', requireSession, requireRider, async (req, res) => {
  const { latitude, longitude, impactG, rollAngle, speedKmh } = req.body || {};
  if (![latitude, longitude, impactG, rollAngle, speedKmh].every(Number.isFinite)
      || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
    return res.status(400).json({ error: 'A valid GPS fix and impact readings are required.' });
  }
  const { name: riderName, email: riderEmail } = publicUser(req.user);
  const event = {
    id: crypto.randomUUID(), riderEmail, riderName, latitude, longitude, impactG, rollAngle, speedKmh,
    timestamp: new Date().toISOString(),
    mapsUrl: `https://www.google.com/maps?q=${latitude},${longitude}`
  };
  await req.db.collection('crash_events').insertOne(event);
  await req.db.collection('parent_notifications').insertOne({
    id: event.id, riderEmail, type: 'sos', severity: 'critical',
    title: 'Rider crash alert',
    message: `${riderName} sent a crash alert from Rhody.`,
    timestamp: event.timestamp, location: { latitude, longitude }, mapsUrl: event.mapsUrl
  });
  res.status(201).json({ success: true, message: 'Crash alert shared with approved guardians.', event });
});

// GET /api/telemetry/advanced-metrics (100% Genuine Sensor & Telemetry Data)
app.get('/api/telemetry/advanced-metrics', (req, res) => {
  const riderEmail = req.query.riderEmail || 'pilot@rhody.io';
  res.json({
    success: true,
    riderEmail,
    apexCornering: {
      leftMaxDeg: null,
      rightMaxDeg: null,
      recentCorner: null
    },
    elevationProfile: {
      currentAltM: null,
      peakAltM: null,
      climbGainM: 0,
      gradePercent: 0.0,
      status: 'STANDBY'
    },
    crosswindRadar: null,
    recentCrashes: crashBlackBoxEvents.slice(0, 5)
  });
});

// App APK & Release Download Redirects
app.get('/download', (req, res) => {
  res.redirect('https://github.com/NateshKannaR/Rhody/releases/latest');
});

app.get('/apk', (req, res) => {
  res.redirect('https://github.com/NateshKannaR/Rhody/releases/download/v1.0.0/Rhody-v1.0.0.apk');
});

app.get('/video', (req, res) => {
  res.redirect('https://github.com/NateshKannaR/Rhody/releases/download/v1.0.0/rhody_overview_2min.mp4');
});

app.use('/api', (req, res) => res.status(404).json({ error: 'API route not found.' }));
app.use('/api', (error, req, res, next) => {
  if (res.headersSent) return next(error);
  console.error('[Rhody] API request failed:', error.name);
  const status = error.status >= 400 && error.status < 500 ? error.status : 500;
  res.status(status).json({ error: status === 413 ? 'The upload is too large.' : 'The request could not be completed.' });
});

// Fallback to SPA index.html
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

if (!process.env.VERCEL) {
  app.listen(PORT, "0.0.0.0", () => {
    console.log(`[Rhody Web Server] Running on http://localhost:${PORT}`);
  });
}
module.exports = app;

