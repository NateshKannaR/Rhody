const express = require('express');
const cors = require('cors');
const { MongoClient, ServerApiVersion, ObjectId } = require('mongodb');
const path = require('path');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;

// MongoDB Connection
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://Natesh:Natesh@cluster0.wwp3oig.mongodb.net/?retryWrites=true&w=majority';
const DB_NAME = 'roadpulse';
const AUTH_SECRET = process.env.AUTH_SECRET || 'roadpulse_secret_key_2026';

let db;
let client;
let clientPromise;

async function getDb() {
  if (db) return db;
  if (!clientPromise) {
    client = new MongoClient(MONGODB_URI, {
      serverApi: {
        version: ServerApiVersion.v1,
        strict: true,
        deprecationErrors: true,
      },
      connectTimeoutMS: 15000,
      socketTimeoutMS: 45000,
      maxPoolSize: 10,
    });
    clientPromise = client.connect().then(async (c) => {
      const database = c.db(DB_NAME);
      try {
        await database.collection('trips').createIndex({ startTime: -1 });
        await database.collection('trips').createIndex({ userEmail: 1, startTime: -1 });
        await database.collection('track_points').createIndex({ tripId: 1, timestamp: 1 });
        await database.collection('users').createIndex({ email: 1 }, { unique: true });
      } catch (err) {
        console.warn('[RoadPulse] Index creation notice:', err.message);
      }
      db = database;
      console.log(`[RoadPulse] Successfully connected to MongoDB Atlas database: ${DB_NAME}`);
      return db;
    }).catch(err => {
      console.error('[RoadPulse] Failed to connect to MongoDB Atlas:', err.message);
      clientPromise = null;
      throw err;
    });
  }
  return await clientPromise;
}

// Initial eager connect attempt
getDb().catch(() => {});

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.static(path.join(__dirname, 'public')));

// Serverless-ready MongoDB injection middleware
app.use(async (req, res, next) => {
  if (req.path.startsWith('/api') && req.path !== '/api/status') {
    try {
      req.db = await getDb();
      next();
    } catch (err) {
      console.error('[RoadPulse] Database error on request:', err.message);
      return res.status(503).json({
        error: 'Database disconnected. Please check MongoDB Atlas IP whitelist (ensure 0.0.0.0/0 is added in Network Access for Vercel cloud hosting).',
        details: err.message
      });
    }
  } else {
    next();
  }
});

// Password & Token Utilities
function hashPassword(password, salt = crypto.randomBytes(16).toString('hex')) {
  const hash = crypto.pbkdf2Sync(password, salt, 1000, 64, 'sha512').toString('hex');
  return { hash, salt };
}

function verifyPassword(password, hash, salt) {
  const checkHash = crypto.pbkdf2Sync(password, salt, 1000, 64, 'sha512').toString('hex');
  return checkHash === hash;
}

function generateToken(user) {
  const payload = Buffer.from(JSON.stringify({
    userId: user._id.toString(),
    email: user.email,
    name: user.name || '',
    iat: Date.now()
  })).toString('base64url');
  const signature = crypto.createHmac('sha256', AUTH_SECRET).update(payload).digest('base64url');
  return `${payload}.${signature}`;
}

function verifyToken(token) {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 2) return null;
  const [payload, signature] = parts;
  const expectedSig = crypto.createHmac('sha256', AUTH_SECRET).update(payload).digest('base64url');
  if (signature !== expectedSig) return null;
  try {
    return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
  } catch (e) {
    return null;
  }
}

// ----------------------------------------------------
// AUTHENTICATION ROUTES
// ----------------------------------------------------

// POST /api/auth/register
app.post('/api/auth/register', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const { email, password, name } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }
    const cleanEmail = email.trim().toLowerCase();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(cleanEmail)) {
      return res.status(400).json({ error: 'Please enter a valid email address' });
    }
    if (password.length < 6) {
      return res.status(400).json({ error: 'Password must be at least 6 characters' });
    }

    const existing = await db.collection('users').findOne({ email: cleanEmail });
    if (existing) {
      return res.status(409).json({ error: 'An account with this email already exists' });
    }

    const { hash, salt } = hashPassword(password);
    const userDoc = {
      email: cleanEmail,
      name: (name || '').trim() || cleanEmail.split('@')[0],
      passwordHash: hash,
      salt: salt,
      createdAt: new Date(),
      lastLoginAt: new Date()
    };

    const insertResult = await db.collection('users').insertOne(userDoc);
    userDoc._id = insertResult.insertedId;
    const token = generateToken(userDoc);

    res.status(201).json({
      success: true,
      message: 'Account created successfully',
      token,
      user: {
        id: userDoc._id.toString(),
        email: userDoc.email,
        name: userDoc.name
      }
    });
  } catch (err) {
    console.error('[Auth Register Error]:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/auth/login
app.post('/api/auth/login', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }
    const cleanEmail = email.trim().toLowerCase();
    const user = await db.collection('users').findOne({ email: cleanEmail });
    if (!user) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    const isValid = verifyPassword(password, user.passwordHash, user.salt);
    if (!isValid) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    await db.collection('users').updateOne(
      { _id: user._id },
      { $set: { lastLoginAt: new Date() } }
    );

    const token = generateToken(user);
    res.json({
      success: true,
      message: 'Login successful',
      token,
      user: {
        id: user._id.toString(),
        email: user.email,
        name: user.name || user.email.split('@')[0]
      }
    });
  } catch (err) {
    console.error('[Auth Login Error]:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/auth/me
app.get('/api/auth/me', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);
  if (!decoded) {
    return res.status(401).json({ error: 'Unauthorized or invalid token' });
  }
  try {
    const user = await db.collection('users').findOne({ email: decoded.email.toLowerCase() });
    if (!user) return res.status(404).json({ error: 'User not found' });
    res.json({
      id: user._id.toString(),
      email: user.email,
      name: user.name || user.email.split('@')[0]
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ----------------------------------------------------
// SYSTEM & TELEMETRY ROUTES
// ----------------------------------------------------

// API: System & MongoDB Status
app.get('/api/status', async (req, res) => {
  let isConnected = false;
  let database = null;
  try {
    database = await getDb();
    isConnected = !!database;
  } catch (e) {
    isConnected = false;
  }
  let tripCount = 0;
  let pointCount = 0;
  let userCount = 0;
  if (isConnected && database) {
    try {
      tripCount = await database.collection('trips').countDocuments();
      pointCount = await database.collection('track_points').countDocuments();
      userCount = await database.collection('users').countDocuments();
    } catch (e) {
      console.error(e);
    }
  }
  res.json({
    status: isConnected ? 'online' : 'disconnected',
    database: DB_NAME,
    cluster: 'cluster0.wwp3oig.mongodb.net',
    totalUsers: userCount,
    totalTrips: tripCount,
    totalPoints: pointCount,
    timestamp: new Date().toISOString()
  });
});

// API: Get trips (filtered by user if requested or authenticated)
app.get('/api/trips', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    let query = {};
    const emailParam = req.query.email;
    if (emailParam) {
      query.userEmail = emailParam.toLowerCase();
    } else {
      const authHeader = req.headers.authorization;
      const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
      const decoded = verifyToken(token);
      if (decoded && decoded.email) {
        query.userEmail = decoded.email.toLowerCase();
      }
    }

    const trips = await db.collection('trips')
      .find(query)
      .sort({ startTime: -1 })
      .toArray();
    res.json(trips);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// API: Get trip details with track points
app.get('/api/trips/:id', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const tripId = req.params.id;
    let query = { _id: tripId };
    if (ObjectId.isValid(tripId)) {
      query = { $or: [{ _id: new ObjectId(tripId) }, { localId: Number(tripId) }, { _id: tripId }] };
    } else if (!isNaN(tripId)) {
      query = { localId: Number(tripId) };
    }

    const trip = await db.collection('trips').findOne(query);
    if (!trip) return res.status(404).json({ error: 'Trip not found' });

    const points = await db.collection('track_points')
      .find({ tripId: trip._id.toString() })
      .sort({ timestamp: 1 })
      .toArray();

    res.json({ ...trip, points });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// In-memory live beacons cache for ultra-fast real-time parent tracking
const liveBeacons = new Map();

// API: Broadcast live telemetry beacon (from rider's Android app)
app.post('/api/trips/:id/live', async (req, res) => {
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
      riderName: riderName || 'Rider',
      status: status || 'RECORDING',
      isMoving: Boolean(isMoving),
      currentSpeedKmh: Number(currentSpeedKmh || 0),
      maxSpeedKmh: Number(maxSpeedKmh || 0),
      latitude: Number(latitude || 0),
      longitude: Number(longitude || 0),
      altitudeMeters: altitudeMeters ? Number(altitudeMeters) : null,
      batteryPercent: batteryPercent ? Number(batteryPercent) : null,
      totalDistanceKm: Number(totalDistanceKm || 0),
      elapsedTimeMs: Number(elapsedTimeMs || 0),
      updatedAt: new Date(),
      timestamp: timestamp || Date.now()
    };

    liveBeacons.set(tripId, beaconData);

    if (db) {
      db.collection('live_beacons').updateOne(
        { tripId },
        { $set: beaconData },
        { upsert: true }
      ).catch(e => console.warn('[Beacon DB write]:', e.message));
    }

    res.json({ success: true, timestamp: beaconData.updatedAt });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// API: Get live telemetry beacon (for Parents / Guardians to monitor)
app.get('/api/trips/:id/live', async (req, res) => {
  try {
    const tripId = req.params.id;
    let beacon = liveBeacons.get(tripId);

    if (!beacon && db) {
      beacon = await db.collection('live_beacons').findOne({ tripId });
    }

    if (!beacon && db) {
      let query = { _id: tripId };
      if (ObjectId.isValid(tripId)) {
        query = { $or: [{ _id: new ObjectId(tripId) }, { localId: Number(tripId) }, { _id: tripId }] };
      }
      const trip = await db.collection('trips').findOne(query);
      if (trip) {
        beacon = {
          tripId: trip._id.toString(),
          riderName: trip.userEmail ? trip.userEmail.split('@')[0] : 'Rider',
          status: trip.status || 'FINISHED',
          isMoving: false,
          currentSpeedKmh: 0,
          maxSpeedKmh: Math.round((trip.maxSpeedMps || 0) * 3.6),
          totalDistanceKm: ((trip.totalDistanceMeters || 0) / 1000).toFixed(2),
          elapsedTimeMs: trip.totalDurationMs || trip.movingDurationMs || 0,
          updatedAt: trip.updatedAt || new Date()
        };
      }
    }

    if (!beacon) {
      return res.status(404).json({ error: 'Live ride beacon not found or expired' });
    }

    res.json(beacon);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});


// API: Sync trip from Android or Web client
app.post('/api/trips/sync', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const { trip, points, userEmail } = req.body;
    if (!trip) return res.status(400).json({ error: 'Missing trip data' });

    let email = userEmail || trip.userEmail;
    if (!email) {
      const authHeader = req.headers.authorization;
      const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
      const decoded = verifyToken(token);
      if (decoded && decoded.email) {
        email = decoded.email;
      }
    }
    if (email) {
      email = email.trim().toLowerCase();
    }

    const tripDoc = {
      ...trip,
      localId: trip.id || trip.localId,
      userEmail: email || null,
      updatedAt: new Date()
    };
    delete tripDoc.id;

    // Upsert trip by title & startTime or existing _id
    const filter = trip._id && ObjectId.isValid(trip._id)
      ? { _id: new ObjectId(trip._id) }
      : { startTime: trip.startTime, title: trip.title, ...(email ? { userEmail: email } : {}) };

    await db.collection('trips').updateOne(
      filter,
      { $set: tripDoc },
      { upsert: true }
    );

    const savedTrip = await db.collection('trips').findOne(filter);
    const savedTripId = savedTrip._id.toString();

    // Store points if provided
    if (Array.isArray(points) && points.length > 0) {
      await db.collection('track_points').deleteMany({ tripId: savedTripId });
      const pointDocs = points.map((p, idx) => ({
        ...p,
        tripId: savedTripId,
        userEmail: email || null,
        pointIndex: idx + 1
      }));
      delete pointDocs.id;
      await db.collection('track_points').insertMany(pointDocs);
    }

    res.json({
      success: true,
      message: 'Trip synced successfully with MongoDB',
      tripId: savedTripId,
      userEmail: email || null
    });
  } catch (err) {
    console.error('Sync error:', err);
    res.status(500).json({ error: err.message });
  }
});

// API: Delete trip
app.delete('/api/trips/:id', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const tripId = req.params.id;
    let query = { _id: tripId };
    if (ObjectId.isValid(tripId)) {
      query = { _id: new ObjectId(tripId) };
    }

    const trip = await db.collection('trips').findOne(query);
    if (trip) {
      const idStr = trip._id.toString();
      await db.collection('track_points').deleteMany({ tripId: idStr });
      await db.collection('trips').deleteOne({ _id: trip._id });
    }

    res.json({ success: true, message: 'Trip deleted' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// API: Export GPX
app.get('/api/trips/:id/export/gpx', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const tripId = req.params.id;
    const trip = await db.collection('trips').findOne(ObjectId.isValid(tripId) ? { _id: new ObjectId(tripId) } : { _id: tripId });
    if (!trip) return res.status(404).send('Trip not found');

    const points = await db.collection('track_points')
      .find({ tripId: trip._id.toString() })
      .sort({ timestamp: 1 })
      .toArray();

    const gpx = buildGpx(trip, points);
    res.setHeader('Content-Type', 'application/gpx+xml');
    res.setHeader('Content-Disposition', `attachment; filename="roadpulse_${trip._id}.gpx"`);
    res.send(gpx);
  } catch (err) {
    res.status(500).send(err.message);
  }
});

// API: Export CSV
app.get('/api/trips/:id/export/csv', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const tripId = req.params.id;
    const trip = await db.collection('trips').findOne(ObjectId.isValid(tripId) ? { _id: new ObjectId(tripId) } : { _id: tripId });
    if (!trip) return res.status(404).send('Trip not found');

    const points = await db.collection('track_points')
      .find({ tripId: trip._id.toString() })
      .sort({ timestamp: 1 })
      .toArray();

    const csv = buildCsv(trip, points);
    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', `attachment; filename="roadpulse_${trip._id}.csv"`);
    res.send(csv);
  } catch (err) {
    res.status(500).send(err.message);
  }
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
    const speedKmh = ((p.speedMps || 0) * 3.6).toFixed(1);
    const distKm = ((p.cumulativeDistanceMeters || 0) / 1000).toFixed(3);
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

