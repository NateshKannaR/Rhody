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
        await database.collection('places').createIndex({ userEmail: 1, createdAt: -1 });
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
      name: user.name || 'Natesh',
      avatar: user.avatar || '',
      bio: user.bio || '',
      bikeModel: user.bikeModel || 'Yamaha MT-09 SP',
      bikeSpecs: user.bikeSpecs || '890cc Triple Cylinder • 119 BHP',
      bikePlate: user.bikePlate || 'TN-38-RH-0909',
      tireSetup: user.tireSetup || 'Pirelli Diablo Rosso IV',
      emergencyContactName: user.emergencyContactName || 'Family Guardian (Mom & Dad)',
      emergencyContactPhone: user.emergencyContactPhone || '+91 98765 43210',
      speedUnit: user.speedUnit || 'kmh'
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/user/profile
app.get('/api/user/profile', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);

  const defaultProfile = {
    id: 'guest',
    name: 'Natesh',
    email: 'pilot@rhody.io',
    avatar: '',
    bio: 'Weekend canyon carver & telemetry enthusiast. Western Ghats apex seeker.',
    bikeModel: 'Yamaha MT-09 SP',
    bikeSpecs: '890cc Crossplane-3 • 119 BHP',
    bikePlate: 'TN-38-RH-0909',
    tireSetup: 'Pirelli Diablo Rosso IV (Cold PSI: F 32 / R 36)',
    emergencyContactName: 'Family Guardian (Mom & Dad)',
    emergencyContactPhone: '+91 98765 43210',
    speedUnit: 'kmh'
  };

  if (!decoded || !db) {
    return res.json({ success: true, profile: defaultProfile });
  }

  try {
    const user = await db.collection('users').findOne({ email: decoded.email.toLowerCase() });
    if (!user) return res.json({ success: true, profile: defaultProfile });
    res.json({
      success: true,
      profile: {
        id: user._id.toString(),
        name: user.name || 'Natesh',
        email: user.email,
        avatar: user.avatar || '',
        bio: user.bio || defaultProfile.bio,
        bikeModel: user.bikeModel || defaultProfile.bikeModel,
        bikeSpecs: user.bikeSpecs || defaultProfile.bikeSpecs,
        bikePlate: user.bikePlate || defaultProfile.bikePlate,
        tireSetup: user.tireSetup || defaultProfile.tireSetup,
        emergencyContactName: user.emergencyContactName || defaultProfile.emergencyContactName,
        emergencyContactPhone: user.emergencyContactPhone || defaultProfile.emergencyContactPhone,
        speedUnit: user.speedUnit || 'kmh'
      }
    });
  } catch (err) {
    res.json({ success: true, profile: defaultProfile });
  }
});

// POST /api/user/profile
app.post('/api/user/profile', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);

  const {
    name, avatar, bio, bikeModel, bikeSpecs, bikePlate,
    tireSetup, emergencyContactName, emergencyContactPhone, speedUnit
  } = req.body;

  const updateFields = { updatedAt: new Date() };
  if (name !== undefined) updateFields.name = String(name).trim();
  if (avatar !== undefined) updateFields.avatar = avatar; // base64 data URL
  if (bio !== undefined) updateFields.bio = String(bio).trim();
  if (bikeModel !== undefined) updateFields.bikeModel = String(bikeModel).trim();
  if (bikeSpecs !== undefined) updateFields.bikeSpecs = String(bikeSpecs).trim();
  if (bikePlate !== undefined) updateFields.bikePlate = String(bikePlate).trim();
  if (tireSetup !== undefined) updateFields.tireSetup = String(tireSetup).trim();
  if (emergencyContactName !== undefined) updateFields.emergencyContactName = String(emergencyContactName).trim();
  if (emergencyContactPhone !== undefined) updateFields.emergencyContactPhone = String(emergencyContactPhone).trim();
  if (speedUnit !== undefined) updateFields.speedUnit = speedUnit;

  if (decoded && decoded.email && db) {
    try {
      await db.collection('users').updateOne(
        { email: decoded.email.toLowerCase() },
        { $set: updateFields }
      );
    } catch (err) {
      console.warn('[Profile Save Warning]:', err.message);
    }
  }

  res.json({
    success: true,
    message: 'Profile updated successfully',
    profile: updateFields
  });
});

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

// GET /api/user/places
app.get('/api/user/places', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);

  if (!db) {
    return res.json({ success: true, places: defaultMotorcyclePlaces });
  }

  try {
    const filter = (decoded && decoded.email)
      ? { $or: [{ userEmail: decoded.email.toLowerCase() }, { userEmail: 'guest' }] }
      : { userEmail: 'guest' };

    const dbPlaces = await db.collection('places').find(filter).sort({ createdAt: -1 }).toArray();
    if (dbPlaces && dbPlaces.length > 0) {
      return res.json({ success: true, places: dbPlaces });
    }
    res.json({ success: true, places: defaultMotorcyclePlaces });
  } catch (err) {
    res.json({ success: true, places: defaultMotorcyclePlaces });
  }
});

// POST /api/user/places
app.post('/api/user/places', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);

  try {
    const { name, category, lat, lng, altitude, notes } = req.body;
    if (!name || lat === undefined || lng === undefined) {
      return res.status(400).json({ error: 'Name, latitude, and longitude are required' });
    }
    const placeDoc = {
      name: String(name).trim(),
      category: category || 'waypoint', // 'garage', 'pass', 'fuel', 'cafe', 'track', 'waypoint'
      lat: parseFloat(lat),
      lng: parseFloat(lng),
      altitude: altitude ? parseFloat(altitude) : null,
      notes: notes ? String(notes).trim() : '',
      userEmail: decoded ? decoded.email.toLowerCase() : 'guest',
      createdAt: new Date()
    };

    if (db) {
      const result = await db.collection('places').insertOne(placeDoc);
      placeDoc._id = result.insertedId;
    } else {
      placeDoc._id = 'local_' + Date.now();
    }

    res.status(201).json({ success: true, place: placeDoc });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE /api/user/places/:id
app.delete('/api/user/places/:id', async (req, res) => {
  if (!db) return res.json({ success: true, message: 'Removed locally' });
  try {
    const id = req.params.id;
    if (id && ObjectId.isValid(id)) {
      await db.collection('places').deleteOne({ _id: new ObjectId(id) });
    }
    res.json({ success: true, message: 'Place removed' });
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

// Real YouTube Search API (Resolves real playable video IDs, titles, thumbnails)
app.get('/api/youtube/search', async (req, res) => {
  const query = req.query.q || 'Yamaha MT-09 review';
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

