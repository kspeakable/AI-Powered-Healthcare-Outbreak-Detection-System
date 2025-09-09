#!/bin/bash

echo "🏥 Setting up AI-Powered Healthcare Outbreak Detection System..."
echo "=================================================================="

# Create directories
mkdir -p healthcare-streaming/kafka healthcare-streaming/java-producer

cd healthcare-streaming

# Download Kafka
echo "📥 Downloading Apache Kafka..."
if [ ! -f "kafka_2.13-3.8.0.tgz" ]; then
    wget https://downloads.apache.org/kafka/2.13-3.8.0/kafka_2.13-3.8.0.tgz || \
    wget https://archive.apache.org/dist/kafka/3.8.0/kafka_2.13-3.8.0.tgz
fi

echo "📦 Extracting Kafka..."
tar -xzf kafka_2.13-3.8.0.tgz

# Configure Kafka for low memory
echo "⚙️ Configuring Kafka for 4GB systems..."
cd kafka_2.13-3.8.0
cp config/server.properties config/server-low-mem.properties
echo "heap.opts=-Xmx512m -Xms512m" >> config/server-low-mem.properties

cd ../java-producer

# Download Java dependencies
echo "📚 Downloading Java libraries..."
wget -q https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/3.5.0/kafka-clients-3.5.0.jar
wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar
wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar

# Download Weka ML library
echo "🧠 Downloading Weka ML library..."
wget -q https://prdownloads.sourceforge.net/weka/weka-3-8-6.zip || \
wget -q https://sourceforge.net/projects/weka/files/weka-3-8/3.8.6/weka-3-8-6.zip/download -O weka-3-8-6.zip

unzip -q weka-3-8-6.zip
cp weka-3-8-6/weka.jar .

# Copy source files
echo "📄 Copying source code..."
cp ../../src/*.java .

# Compile Java files
echo "🔨 Compiling Java applications..."
javac -cp "kafka-clients-3.5.0.jar:slf4j-simple-1.7.36.jar:slf4j-api-1.7.36.jar:weka.jar:." HealthcareProducer.java
javac -cp "kafka-clients-3.5.0.jar:slf4j-simple-1.7.36.jar:slf4j-api-1.7.36.jar:weka.jar:." MLOutbreakPredictor.java

cd ../../

echo ""
echo "✅ Setup completed successfully!"
echo ""
echo "🚀 To start the system:"
echo "1. ./start-kafka.sh      (Start Kafka services)"
echo "2. ./run-ai-predictor.sh (Start AI outbreak predictor)"
echo "3. ./run-data-generator.sh (Start healthcare data generator)"
echo ""
echo "📊 The system will begin real-time outbreak detection with AI predictions!"
