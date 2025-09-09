#!/bin/bash

echo "🤖 Starting AI-Powered Outbreak Predictor..."
echo "============================================="

cd healthcare-streaming/java-producer

echo "🧠 Initializing ML components..."
echo "📊 Ready to learn outbreak patterns from live data"
echo ""

java -cp "kafka-clients-3.5.0.jar:slf4j-simple-1.7.36.jar:slf4j-api-1.7.36.jar:weka.jar:." MLOutbreakPredictor
