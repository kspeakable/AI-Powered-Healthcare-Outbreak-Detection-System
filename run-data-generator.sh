#!/bin/bash

echo "🏥 Starting Healthcare Data Generator..."
echo "========================================"

cd healthcare-streaming/java-producer

echo "📡 Generating patient data from 5 hospitals..."
echo "⚡ Simulating respiratory illness patterns..."
echo "🔄 Creating outbreak clusters every 50 patients..."
echo ""

java -cp "kafka-clients-3.5.0.jar:slf4j-simple-1.7.36.jar:slf4j-api-1.7.36.jar:." HealthcareProducer
