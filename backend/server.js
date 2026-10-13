const express = require('express');
const cors = require('cors');
const { MongoClient, ServerApiVersion, ObjectId } = require('mongodb');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// MongoDB Connection
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://Natesh:Natesh@cluster0.wwp3oig.mongodb.net/?retryWrites=true&w=majority';
const DB_NAME = 'roadpulse';

let db;
let client;

async function connectToMongo() {
  try {
    client = new MongoClient(MONGODB_URI, {
      serverApi: {
        version: ServerApiVersion.v1,
        strict: true,
        deprecationErrors: true,
      }
    });
    await client.connect();
    db = client.db(DB_NAME);
    // Create indexes for high query performance
    await db.collection('trips').createIndex({ startTime: -1 });
    await db.collection('track_points').createIndex({ tripId: 1, timestamp: 1 });
    console.log(`[RoadPulse] Successfully connected to MongoDB Atlas database: ${DB_NAME}`);
  } catch (err) {
    console.error('[RoadPulse] Failed to connect to MongoDB Atlas:', err.message);
  }
}
connectToMongo();

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.static(path.join(__dirname, 'public')));

// API: System & MongoDB Status
app.get('/api/status', async (req, res) => {
  const isConnected = !!db;
  let tripCount = 0;
  let pointCount = 0;
  if (isConnected) {
    try {
      tripCount = await db.collection('trips').countDocuments();
      pointCount = await db.collection('track_points').countDocuments();
    } catch (e) {
      console.error(e);
    }
  }
  res.json({
    status: isConnected ? 'online' : 'disconnected',
    database: DB_NAME,
    cluster: 'cluster0.wwp3oig.mongodb.net',
    totalTrips: tripCount,
    totalPoints: pointCount,
    timestamp: new Date().toISOString()
  });
});

// API: Get all trips
app.get('/api/trips', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const trips = await db.collection('trips')
      .find({})
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

// API: Sync trip from Android or Web client
app.post('/api/trips/sync', async (req, res) => {
  if (!db) return res.status(503).json({ error: 'Database disconnected' });
  try {
    const { trip, points } = req.body;
    if (!trip) return res.status(400).json({ error: 'Missing trip data' });

    const tripDoc = {
      ...trip,
      localId: trip.id || trip.localId,
      updatedAt: new Date()
    };
    delete tripDoc.id;

    // Upsert trip by title & startTime or existing _id
    const filter = trip._id && ObjectId.isValid(trip._id)
      ? { _id: new ObjectId(trip._id) }
      : { startTime: trip.startTime, title: trip.title };

    const tripResult = await db.collection('trips').updateOne(
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
        pointIndex: idx + 1
      }));
      delete pointDocs.id;
      await db.collection('track_points').insertMany(pointDocs);
    }

    res.json({
      success: true,
      message: 'Trip synced successfully with MongoDB',
      tripId: savedTripId
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
  xml += `<gpx version="1.1" creator="RoadPulse Cloud" xmlns="http://www.topografix.com/GPX/1/1">\n`;
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
  let csv = `# RoadPulse Cloud Export\n# Title: ${trip.title}\n# Start: ${new Date(trip.startTime).toISOString()}\n`;
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

// Fallback to SPA index.html
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`[RoadPulse Web Server] Running on http://localhost:${PORT}`);
});
