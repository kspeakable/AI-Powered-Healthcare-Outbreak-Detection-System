#!/bin/bash

echo "🔥 Starting Kafka Services for Healthcare Monitoring..."
echo "======================================================"

cd healthcare-streaming/kafka_2.13-3.8.0

# Start Zookeeper in background
echo "🌳 Starting Zookeeper..."
./bin/zookeeper-server-start.sh config/zookeeper.properties &
ZOOKEEPER_PID=$!
echo "Zookeeper PID: $ZOOKEEPER_PID"

# Wait for Zookeeper to start
sleep 5

# Start Kafka Server
echo "⚡ Starting Kafka Server..."
./bin/kafka-server-start.sh config/server-low-mem.properties &
KAFKA_PID=$!
echo "Kafka PID: $KAFKA_PID"

# Wait for Kafka to start
sleep 10

# Create topic
echo "📋 Creating patient-reports topic..."
./bin/kafka-topics.sh --create --topic patient-reports --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# Verify topic creation
echo "✅ Verifying topic creation..."
./bin/kafka-topics.sh --list --bootstrap-server localhost:9092

echo ""
echo "🚀 Kafka is ready!"
echo "Zookeeper PID: $ZOOKEEPER_PID"
echo "Kafka PID: $KAFKA_PID"
echo ""
echo "To stop services:"
echo "kill $ZOOKEEPER_PID $KAFKA_PID"

# Keep script running
wait
