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
      // For parent and bike management routes, allow graceful offline fallback
      if (req.path.startsWith('/api/parent') || req.path.startsWith('/api/user/bike-management') || req.path.startsWith('/api/trips')) {
        req.db = null;
        return next();
      }
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
    const { email, password, name, role, wardCodeOrEmail, bikeModel, bikePlate, emergencyContactPhone } = req.body;
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

    const userRole = (role === 'parent' || role === 'guardian') ? 'parent' : 'rider';
    const wardClean = wardCodeOrEmail ? String(wardCodeOrEmail).trim().toLowerCase() : '';

    if (userRole === 'parent') {
      if (!wardClean) {
        return res.status(400).json({ error: 'Please enter the ward rider code or email to monitor' });
      }
      if (cleanEmail === wardClean) {
        return res.status(400).json({ error: 'Guardian email cannot be the same as rider email! A guardian and rider must be separate individuals.' });
      }
      const existingRider = await db.collection('users').findOne({ email: cleanEmail, role: 'rider' });
      if (existingRider) {
        return res.status(400).json({ error: 'This email is already registered as a Rider. Guardians must use a distinct email address.' });
      }
    }

    const existing = await db.collection('users').findOne({ email: cleanEmail });
    if (existing) {
      return res.status(409).json({ error: 'An account with this email already exists' });
    }

    const { hash, salt } = hashPassword(password);
    const userDoc = {
      email: cleanEmail,
      name: (name || '').trim() || cleanEmail.split('@')[0],
      role: userRole,
      wardEmail: wardClean,
      passwordHash: hash,
      salt: salt,
      bikeModel: bikeModel || (userRole === 'rider' ? 'Bajaj Pulsar 150' : ''),
      bikePlate: bikePlate || (userRole === 'rider' ? 'MH-12-RP-8821' : ''),
      emergencyContactPhone: emergencyContactPhone || '',
      createdAt: new Date(),
      lastLoginAt: new Date()
    };

    const insertResult = await db.collection('users').insertOne(userDoc);
    userDoc._id = insertResult.insertedId;

    if (userRole === 'parent' && db) {
      await db.collection('guardian_connections').updateOne(
        { parentEmail: cleanEmail },
        {
          $set: {
            parentEmail: cleanEmail,
            parentName: userDoc.name,
            riderEmail: wardClean,
            status: 'approved',
            approvedAt: new Date(),
            updatedAt: new Date()
          }
        },
        { upsert: true }
      );
    }

    const token = generateToken(userDoc);

    res.status(201).json({
      success: true,
      message: 'Account created successfully',
      token,
      user: {
        id: userDoc._id.toString(),
        email: userDoc.email,
        name: userDoc.name,
        role: userDoc.role,
        wardEmail: userDoc.wardEmail || '',
        avatar: userDoc.avatar || '',
        bio: userDoc.bio || '',
        bikeModel: userDoc.bikeModel || '',
        bikeSpecs: userDoc.bikeSpecs || '',
        bikePlate: userDoc.bikePlate || '',
        tireSetup: userDoc.tireSetup || '',
        emergencyContactName: userDoc.emergencyContactName || '',
        emergencyContactPhone: userDoc.emergencyContactPhone || '',
        speedUnit: userDoc.speedUnit || 'kmh'
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
      name: user.name || 'Pilot',
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
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

let guestProfileCache = {
  id: 'guest',
  name: 'Pilot',
  email: '',
  avatar: '',
  bio: 'Precision motorcycle telemetry & dynamic apex trajectory tracking.',
  bikeModel: '',
  bikeSpecs: '',
  bikePlate: '',
  tireSetup: '',
  emergencyContactName: '',
  emergencyContactPhone: '',
  speedUnit: 'kmh'
};

// GET /api/user/profile
app.get('/api/user/profile', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);
  const targetEmail = (decoded && decoded.email) ? decoded.email.toLowerCase() : 'pilot@rhody.io';

  if (!db) {
    return res.json({ success: true, profile: guestProfileCache });
  }

  try {
    const user = await db.collection('users').findOne({ email: targetEmail });
    if (!user) {
      return res.json({ success: true, profile: guestProfileCache });
    }
    const profile = {
      id: user._id ? user._id.toString() : 'guest',
      name: user.name || guestProfileCache.name,
      email: user.email || targetEmail,
      avatar: (user.avatar !== undefined && user.avatar !== '') ? user.avatar : guestProfileCache.avatar,
      bio: user.bio || guestProfileCache.bio,
      bikeModel: user.bikeModel || guestProfileCache.bikeModel,
      bikeSpecs: user.bikeSpecs || guestProfileCache.bikeSpecs,
      bikePlate: user.bikePlate || guestProfileCache.bikePlate,
      tireSetup: user.tireSetup || guestProfileCache.tireSetup,
      emergencyContactName: user.emergencyContactName || guestProfileCache.emergencyContactName,
      emergencyContactPhone: user.emergencyContactPhone || guestProfileCache.emergencyContactPhone,
      speedUnit: user.speedUnit || guestProfileCache.speedUnit || 'kmh'
    };
    if (targetEmail === 'pilot@rhody.io' && profile.avatar) {
      guestProfileCache.avatar = profile.avatar;
    }
    res.json({ success: true, profile });
  } catch (err) {
    res.json({ success: true, profile: guestProfileCache });
  }
});

// POST & PUT /api/user/profile
const handleProfileUpdate = async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);
  const targetEmail = (decoded && decoded.email) ? decoded.email.toLowerCase() : 'pilot@rhody.io';

  const {
    name, avatar, bio, bikeModel, bikeSpecs, bikePlate,
    tireSetup, emergencyContactName, emergencyContactPhone, speedUnit
  } = req.body;

  const updateFields = { updatedAt: new Date() };
  if (name !== undefined) updateFields.name = String(name).trim();
  if (avatar !== undefined) updateFields.avatar = avatar; // base64 data URL or empty string
  if (bio !== undefined) updateFields.bio = String(bio).trim();
  if (bikeModel !== undefined) updateFields.bikeModel = String(bikeModel).trim();
  if (bikeSpecs !== undefined) updateFields.bikeSpecs = String(bikeSpecs).trim();
  if (bikePlate !== undefined) updateFields.bikePlate = String(bikePlate).trim();
  if (tireSetup !== undefined) updateFields.tireSetup = String(tireSetup).trim();
  if (emergencyContactName !== undefined) updateFields.emergencyContactName = String(emergencyContactName).trim();
  if (emergencyContactPhone !== undefined) updateFields.emergencyContactPhone = String(emergencyContactPhone).trim();
  if (speedUnit !== undefined) updateFields.speedUnit = speedUnit;

  guestProfileCache = Object.assign({}, guestProfileCache, updateFields);

  if (db) {
    try {
      await db.collection('users').updateOne(
        { email: targetEmail },
        { $set: { ...updateFields, email: targetEmail } },
        { upsert: true }
      );
    } catch (err) {
      console.warn('[Profile Save Warning]:', err.message);
    }
  }

  res.json({
    success: true,
    message: 'Profile updated successfully',
    profile: guestProfileCache
  });
};

app.post('/api/user/profile', handleProfileUpdate);
app.put('/api/user/profile', handleProfileUpdate);

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

let guestBikeManagementStore = JSON.parse(JSON.stringify(defaultBikeManagement));

// GET /api/user/bike-management
app.get('/api/user/bike-management', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);
  const userEmail = decoded ? decoded.email.toLowerCase() : 'guest';

  if (!db) {
    return res.json({ success: true, data: guestBikeManagementStore });
  }

  try {
    const doc = await db.collection('bike_management').findOne({ userEmail });
    if (doc && doc.data) {
      if (userEmail === 'guest') {
        guestBikeManagementStore = doc.data;
      }
      return res.json({ success: true, data: doc.data });
    }
    res.json({ success: true, data: guestBikeManagementStore });
  } catch (err) {
    res.json({ success: true, data: guestBikeManagementStore });
  }
});

// POST /api/user/bike-management
app.post('/api/user/bike-management', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);
  const userEmail = decoded ? decoded.email.toLowerCase() : 'guest';

  try {
    const mgmtData = req.body;
    if (userEmail === 'guest') {
      guestBikeManagementStore = Object.assign({}, guestBikeManagementStore, mgmtData);
    }
    if (db) {
      await db.collection('bike_management').updateOne(
        { userEmail },
        { $set: { userEmail, data: mgmtData, updatedAt: new Date() } },
        { upsert: true }
      );
    }
    res.json({ success: true, message: 'Motorcycle management records updated', data: mgmtData });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE /api/user/places/:id (Clean fallback for legacy calls)
app.delete('/api/user/places/:id', async (req, res) => {
  res.json({ success: true, message: 'Saved places removed' });
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
let parentNotifications = [];

// Two-Way Guardian Consent & Connection State
let guardianConnections = new Map();

// POST /api/parent/login
app.post('/api/parent/login', async (req, res) => {
  const { email, riderCode } = req.body;
  const parentEmail = (email && String(email).trim().toLowerCase()) || 'guardian@family.io';
  const targetRider = 'pilot@rhody.io';

  let conn = guardianConnections.get(parentEmail);
  if (!conn) {
    conn = {
      id: 'req_' + parentEmail.replace(/[^a-zA-Z0-9]/g, '_'),
      parentEmail,
      parentName: 'Family Guardian (Dad & Mom)',
      riderEmail: targetRider,
      status: 'pending_approval',
      requestedAt: new Date().toISOString(),
      approvedAt: null
    };
    guardianConnections.set(parentEmail, conn);
  }

  const parentProfile = {
    parentEmail,
    parentName: conn.parentName,
    connectedRiderEmail: targetRider,
    connectedRiderName: 'Natesh',
    status: conn.status,
    role: 'parent',
    token: 'parent_token_' + Date.now()
  };

  res.json({
    success: true,
    message: 'Parent / Guardian login successful',
    profile: parentProfile,
    isApproved: conn.status === 'approved'
  });
});

// POST /api/parent/connect-rider
app.post('/api/parent/connect-rider', async (req, res) => {
  const { parentEmail, parentName, riderCodeOrEmail } = req.body;
  const pEmail = (parentEmail && String(parentEmail).trim().toLowerCase()) || 'guardian@family.io';
  const rEmail = (riderCodeOrEmail && String(riderCodeOrEmail).trim().toLowerCase().includes('@'))
    ? riderCodeOrEmail.trim().toLowerCase()
    : 'pilot@rhody.io';

  const connection = {
    id: 'req_' + pEmail.replace(/[^a-zA-Z0-9]/g, '_'),
    parentEmail: pEmail,
    parentName: parentName || 'Family Guardian (Dad & Mom)',
    riderEmail: rEmail,
    status: 'pending_approval',
    requestedAt: new Date().toISOString(),
    approvedAt: null
  };
  guardianConnections.set(pEmail, connection);

  // High-priority notification for rider to accept
  const riderReqNotif = {
    id: 'req_' + Date.now(),
    riderEmail: rEmail,
    type: 'guardian_request',
    title: 'Guardian Link Request',
    message: `${connection.parentName} (${pEmail}) is requesting live safety tracking access.`,
    guardianEmail: pEmail,
    guardianName: connection.parentName,
    severity: 'warning',
    timestamp: new Date().toISOString()
  };
  parentNotifications.unshift(riderReqNotif);

  res.json({
    success: true,
    status: 'pending_approval',
    message: `Connection request sent to rider ${rEmail}. Awaiting rider authorization in their cockpit.`,
    connection
  });
});

// GET /api/rider/guardian-requests
app.get('/api/rider/guardian-requests', (req, res) => {
  const riderEmail = req.query.riderEmail || 'pilot@rhody.io';
  const requests = [];
  for (const conn of guardianConnections.values()) {
    if (!conn.riderEmail || conn.riderEmail === riderEmail) {
      requests.push(conn);
    }
  }
  res.json({ success: true, requests });
});

// POST /api/rider/respond-guardian-request
app.post('/api/rider/respond-guardian-request', (req, res) => {
  const { parentEmail, requestId, action } = req.body; // action: 'accept' | 'approved' | 'reject' | 'rejected' | 'revoke'
  let pEmail = (parentEmail && String(parentEmail).trim().toLowerCase());

  if (!pEmail && requestId) {
    for (const [email, conn] of guardianConnections.entries()) {
      if (conn.id === requestId) {
        pEmail = email;
        break;
      }
    }
  }
  if (!pEmail) pEmail = 'guardian@family.io';

  let conn = guardianConnections.get(pEmail);
  if (!conn) {
    conn = {
      id: requestId || ('req_' + pEmail.replace(/[^a-zA-Z0-9]/g, '_')),
      parentEmail: pEmail,
      parentName: 'Family Guardian',
      riderEmail: 'pilot@rhody.io',
      requestedAt: new Date().toISOString()
    };
  }

  const isApproved = (action === 'accept' || action === 'approved');
  if (isApproved) {
    conn.status = 'approved';
    conn.approvedAt = new Date().toISOString();
    guardianConnections.set(pEmail, conn);

    parentNotifications.unshift({
      id: 'notif_accept_' + Date.now(),
      riderEmail: conn.riderEmail,
      type: 'info',
      title: 'Guardian Access Granted',
      message: `Rider approved live tracking for ${conn.parentName}. Live radar telemetry and documents are now unlocked.`,
      timestamp: new Date().toISOString(),
      severity: 'info'
    });

    return res.json({
      success: true,
      message: `Access granted to ${conn.parentName}`,
      status: 'approved',
      connection: conn
    });
  } else {
    conn.status = action === 'revoke' ? 'revoked' : 'rejected';
    conn.revokedAt = new Date().toISOString();
    guardianConnections.set(pEmail, conn);

    return res.json({
      success: true,
      message: `Access ${action}ed for ${conn.parentName}`,
      status: conn.status,
      connection: conn
    });
  }
});

// GET /api/parent/rider-status
app.get('/api/parent/rider-status', async (req, res) => {
  try {
    const riderEmail = req.query.riderEmail || 'pilot@rhody.io';
    const parentEmail = (req.query.parentEmail && String(req.query.parentEmail).trim().toLowerCase()) || 'guardian@family.io';

    let conn = null;
    if (db) {
      conn = await db.collection('guardian_connections').findOne({ parentEmail });
    }
    if (!conn) {
      conn = guardianConnections.get(parentEmail);
    }
    const isApproved = !conn || conn.status !== 'rejected';

    let rider = null;
    if (db) {
      rider = await db.collection('users').findOne({ email: riderEmail });
    }
    const profile = rider || guestProfileCache;

    let bikeMgmt = null;
    if (db) {
      const doc = await db.collection('bike_management').findOne({ userEmail: 'guest' });
      if (doc && doc.data) bikeMgmt = doc.data;
    }
    if (!bikeMgmt) bikeMgmt = guestBikeManagementStore;

    let latestBeacon = null;
    for (const beacon of liveBeacons.values()) {
      latestBeacon = beacon;
      break;
    }
    if (!latestBeacon && db) {
      latestBeacon = await db.collection('live_beacons').findOne({}, { sort: { updatedAt: -1 } });
    }

    if (!latestBeacon) {
      latestBeacon = {
        tripId: 'live-safety-beacon',
        riderName: profile.name || 'Natesh',
        status: 'PARKED',
        isMoving: false,
        currentSpeedKmh: 0,
        maxSpeedKmh: 86.4,
        latitude: 12.9716,
        longitude: 77.5946,
        altitudeMeters: 920,
        batteryPercent: 88,
        totalDistanceKm: 0,
        updatedAt: new Date()
      };
    }

    let recentTrips = [];
    if (db) {
      recentTrips = await db.collection('trips')
        .find({})
        .sort({ startTime: -1 })
        .limit(5)
        .toArray();
    }

    res.json({
      success: true,
      authorized: true,
      status: 'approved',
      rider: {
        name: profile.name || 'Natesh',
        email: riderEmail,
        avatar: profile.avatar || '',
        bikeModel: profile.bikeModel || (bikeMgmt.specs && bikeMgmt.specs.makeModel) || '',
        bikePlate: profile.bikePlate || (bikeMgmt.documents && (bikeMgmt.documents.rcNumber || (bikeMgmt.documents.rc && bikeMgmt.documents.rc.number))) || '',
        odometer: (bikeMgmt.specs && bikeMgmt.specs.currentOdometer) || 0
      },
      documents: bikeMgmt.documents || {},
      bike: bikeMgmt,
      telemetry: latestBeacon,
      beacon: latestBeacon,
      recentTrips: recentTrips.map(t => ({
        id: t._id ? t._id.toString() : t.id,
        name: t.name || 'Ride Telemetry Session',
        distanceKm: t.distanceKm || (t.totalDistanceMeters ? (t.totalDistanceMeters / 1000).toFixed(1) : 0),
        durationFormatted: t.durationFormatted || '25 min',
        maxSpeedKmh: t.maxSpeedKmh || (t.topSpeedKmh ? t.topSpeedKmh.toFixed(1) : 0),
        startTime: t.startTime
      })),
      trips: recentTrips,
      notifications: notifs
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/parent/notifications
app.get('/api/parent/notifications', (req, res) => {
  const riderEmail = req.query.riderEmail || 'pilot@rhody.io';
  const list = parentNotifications.filter(n => !n.riderEmail || n.riderEmail === riderEmail);
  res.json({ success: true, notifications: list });
});

// POST /api/parent/notifications
app.post('/api/parent/notifications', async (req, res) => {
  const { type, title, message, severity, riderEmail, latitude, longitude, speedKmh } = req.body;
  const newNotif = {
    id: 'notif_' + Date.now(),
    riderEmail: riderEmail || 'pilot@rhody.io',
    riderName: 'Natesh',
    type: type || 'alert',
    title: title || 'Safety Alert',
    message: message || '',
    severity: severity || 'warning',
    latitude: latitude || null,
    longitude: longitude || null,
    speedKmh: speedKmh || null,
    timestamp: new Date().toISOString(),
    read: false
  };

  parentNotifications.unshift(newNotif);
  if (parentNotifications.length > 50) parentNotifications.pop();

  if (db) {
    db.collection('parent_notifications').insertOne(newNotif).catch(() => {});
  }

  res.json({ success: true, notification: newNotif });
});

// DELETE /api/parent/notifications/:id
app.delete('/api/parent/notifications/:id', (req, res) => {
  const id = req.params.id;
  if (id === 'all') {
    parentNotifications = [];
  } else {
    parentNotifications = parentNotifications.filter(n => n.id !== id);
  }
  res.json({ success: true, message: 'Notification dismissed' });
});

// ========================================================
// ADVANCED MOTORCYCLE TELEMETRY & CRASH BLACK BOX ROUTES
// ========================================================

let crashBlackBoxEvents = [];

// POST /api/emergency/crash-event
app.post('/api/emergency/crash-event', async (req, res) => {
  try {
    const {
      riderEmail = 'pilot@rhody.io',
      riderName = 'Natesh',
      latitude = 12.9716,
      longitude = 77.5946,
      impactG = 4.2,
      rollAngle = 78.5,
      speedKmh = 54.2,
      timestamp = new Date().toISOString()
    } = req.body;

    const crashEvent = {
      id: 'crash_' + Date.now(),
      riderEmail,
      riderName,
      latitude: Number(latitude),
      longitude: Number(longitude),
      impactG: Number(impactG),
      rollAngle: Number(rollAngle),
      speedKmh: Number(speedKmh),
      timestamp,
      status: 'CRITICAL_DISPATCHED',
      mapsUrl: `https://www.google.com/maps?q=${latitude},${longitude}`
    };

    crashBlackBoxEvents.unshift(crashEvent);
    if (crashBlackBoxEvents.length > 50) crashBlackBoxEvents.pop();

    if (db) {
      db.collection('crash_events').insertOne(crashEvent).catch(() => {});
    }

    // Immediately push high-priority critical SOS alert into Guardian Notifications Feed
    const guardianEmergencyNotif = {
      id: 'alert_crash_' + Date.now(),
      riderEmail,
      type: 'sos',
      title: 'CRITICAL CRASH DETECTED',
      message: `Rider ${riderName} detected in high-G impact (${impactG}G, ${rollAngle} deg tilt) at ${speedKmh} km/h. Location: ${latitude.toFixed(5)}, ${longitude.toFixed(5)}. Emergency rescue coordinates dispatched!`,
      severity: 'critical',
      timestamp: new Date().toISOString(),
      location: { latitude, longitude },
      mapsUrl: crashEvent.mapsUrl
    };

    parentNotifications.unshift(guardianEmergencyNotif);
    if (parentNotifications.length > 50) parentNotifications.pop();

    if (db) {
      db.collection('parent_notifications').insertOne(guardianEmergencyNotif).catch(() => {});
    }

    // Broadcast live beacon distress status
    for (const beacon of liveBeacons.values()) {
      beacon.status = 'CRASH_IMPACT_DETECTED';
      beacon.currentSpeedKmh = 0;
      beacon.isMoving = false;
      beacon.latitude = Number(latitude);
      beacon.longitude = Number(longitude);
      beacon.updatedAt = new Date();
    }

    res.status(201).json({
      success: true,
      message: 'Crash emergency beacon broadcasted to guardian network and black box recorded',
      event: crashEvent
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
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

