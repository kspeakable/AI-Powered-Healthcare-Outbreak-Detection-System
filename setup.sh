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
echo "🧠 Setting up Weka ML library..."
if [ ! -f "weka.jar" ]; then
    echo "   Checking for existing Weka installation..."
    
    # First try to copy from working installation
    if [ -f "$HOME/healthcare-streaming/java-producer/weka.jar" ]; then
        echo "   Found working Weka JAR, copying..."
        cp "$HOME/healthcare-streaming/java-producer/weka.jar" .
        echo "   ✅ Weka JAR copied successfully"
    else
        echo "   Attempting download..."
        # Only try download if copy failed
        if wget -q --timeout=15 https://prdownloads.sourceforge.net/weka/weka-3-8-6.zip; then
            echo "   Download successful, extracting..."
            if unzip -q weka-3-8-6.zip && [ -f "weka-3-8-6/weka.jar" ]; then
                cp weka-3-8-6/weka.jar .
                echo "   ✅ Weka extracted successfully"
            else
                echo "   ❌ Weka extraction failed"
                echo "   Please manually copy weka.jar to this directory"
            fi
        else
            echo "   ❌ Download failed - please manually copy weka.jar"
        fi
    fi
else
    echo "   ✅ Weka already present"
fi

# Verify Weka is available
if [ -f "weka.jar" ]; then
    echo "   ✅ Weka JAR verified"
else
    echo "   ❌ Weka JAR missing - compilation will fail"
    exit 1
fi

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
