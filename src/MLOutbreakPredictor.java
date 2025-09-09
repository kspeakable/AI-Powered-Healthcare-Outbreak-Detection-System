import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import weka.core.*;
import weka.classifiers.functions.Logistic;
import weka.classifiers.Evaluation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

public class MLOutbreakPredictor {
    private static final String TOPIC_NAME = "patient-reports";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    
    // ML Model and Data
    private static Logistic outbreakModel;
    private static Instances trainingData;
    private static boolean modelTrained = false;
    
    // Hospital tracking for features
    private static Map<String, List<PatientCase>> hospitalCases = new ConcurrentHashMap<>();
    private static Map<String, Double> hospitalRiskScores = new ConcurrentHashMap<>();
    private static Map<String, HospitalMetrics> hospitalMetrics = new ConcurrentHashMap<>();
    private static long lastAnalysisTime = System.currentTimeMillis();
    private static long lastMLUpdate = System.currentTimeMillis();
    
    static class PatientCase {
        String patientId, hospital, symptoms, severity;
        double temperature;
        long timestamp;
        
        PatientCase(String patientId, String hospital, String symptoms, String severity, double temperature, long timestamp) {
            this.patientId = patientId;
            this.hospital = hospital;
            this.symptoms = symptoms;
            this.severity = severity;
            this.temperature = temperature;
            this.timestamp = timestamp;
        }
    }
    
    static class HospitalMetrics {
        double caseVelocity = 0.0;         // cases per minute
        double avgTemperature = 98.6;       // average temperature
        double symptomDiversity = 0.0;      // number of different symptoms
        double severeCaseRatio = 0.0;       // ratio of severe cases
        int totalCases = 0;
        long lastUpdateTime = System.currentTimeMillis();
        
        // Historical outbreak indicator (for training labels)
        boolean hadRecentOutbreak = false;
    }
    
    public static void main(String[] args) {
        // Initialize ML components
        initializeMLModel();
        
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ml-outbreak-predictor");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(TOPIC_NAME));
        
        System.out.println("🤖 AI-POWERED OUTBREAK PREDICTION SYSTEM 🤖");
        System.out.println("Machine Learning + Real-time Healthcare Monitoring");
        System.out.println("Learning outbreak patterns from live data...");
        System.out.println("=" .repeat(70));
        
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, String> record : records) {
                    processPatientReport(record.value());
                }
                
                // Regular outbreak analysis
                if (System.currentTimeMillis() - lastAnalysisTime > 8000) {
                    performOutbreakAnalysis();
                    lastAnalysisTime = System.currentTimeMillis();
                }
                
                // ML model updates
                if (System.currentTimeMillis() - lastMLUpdate > 15000) {
                    updateMLModel();
                    generateRiskPredictions();
                    lastMLUpdate = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in ML outbreak prediction: " + e.getMessage());
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
    
    private static void initializeMLModel() {
        try {
            // Create attribute structure for ML model
            ArrayList<Attribute> attributes = new ArrayList<>();
            attributes.add(new Attribute("caseVelocity"));      // cases per minute
            attributes.add(new Attribute("avgTemperature"));    // average temp
            attributes.add(new Attribute("symptomDiversity"));  // symptom variety
            attributes.add(new Attribute("severeCaseRatio"));   // severe case ratio
            
            // Output class (outbreak risk: low, medium, high)
            ArrayList<String> classValues = new ArrayList<>();
            classValues.add("LOW");
            classValues.add("MEDIUM");
            classValues.add("HIGH");
            attributes.add(new Attribute("outbreakRisk", classValues));
            
            // Create dataset structure
            trainingData = new Instances("OutbreakData", attributes, 0);
            trainingData.setClassIndex(trainingData.numAttributes() - 1);
            
            // Initialize ML model
            outbreakModel = new Logistic();
            
            System.out.println("🧠 ML Model initialized - Ready to learn outbreak patterns!");
        } catch (Exception e) {
            System.err.println("Error initializing ML model: " + e.getMessage());
        }
    }
    
    private static void processPatientReport(String reportJson) {
        try {
            // Parse patient data
            String patientId = extractValue(reportJson, "patientId");
            String hospital = extractValue(reportJson, "hospital");
            String symptoms = extractValue(reportJson, "symptoms");
            String severity = extractValue(reportJson, "severity");
            double temperature = Double.parseDouble(extractValue(reportJson, "temperature"));
            long timestamp = Long.parseLong(extractValue(reportJson, "timestamp"));
            
            PatientCase patientCase = new PatientCase(patientId, hospital, symptoms, severity, temperature, timestamp);
            
            // Add to hospital tracking
            hospitalCases.computeIfAbsent(hospital, k -> new ArrayList<>()).add(patientCase);
            
            // Update hospital metrics for ML features
            updateHospitalMetrics(hospital, patientCase);
            
            // Show incoming data
            System.out.println("📝 " + hospital + " - " + symptoms + " (Temp: " + temperature + "°F, " + severity + ")");
            
            // Immediate critical alerts
            if (temperature > 103.0) {
                System.out.println("🔥 CRITICAL FEVER: " + hospital + " - " + temperature + "°F");
            }
            
            // Check for outbreak clusters
            boolean isCluster = reportJson.contains("\"cluster\":true");
            if (isCluster) {
                hospitalMetrics.get(hospital).hadRecentOutbreak = true;
                System.out.println("⚠️ OUTBREAK CLUSTER DETECTED: " + hospital);
            }
            
        } catch (Exception e) {
            System.err.println("Error processing patient report: " + e.getMessage());
        }
    }
    
    private static void updateHospitalMetrics(String hospital, PatientCase patientCase) {
        HospitalMetrics metrics = hospitalMetrics.computeIfAbsent(hospital, k -> new HospitalMetrics());
        
        long currentTime = System.currentTimeMillis();
        long timeWindow = 60000; // 1 minute window
        
        // Get recent cases for this hospital
        List<PatientCase> recentCases = hospitalCases.get(hospital).stream()
            .filter(c -> currentTime - c.timestamp <= timeWindow)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        if (!recentCases.isEmpty()) {
            // Update metrics
            metrics.caseVelocity = recentCases.size(); // cases per minute
            metrics.avgTemperature = recentCases.stream().mapToDouble(c -> c.temperature).average().orElse(98.6);
            metrics.symptomDiversity = recentCases.stream().map(c -> c.symptoms).distinct().count();
            metrics.severeCaseRatio = (double) recentCases.stream()
                .filter(c -> "severe".equals(c.severity) || "critical".equals(c.severity))
                .count() / recentCases.size();
            metrics.totalCases++;
            metrics.lastUpdateTime = currentTime;
        }
    }
    
    private static void performOutbreakAnalysis() {
        System.out.println("\n📊 OUTBREAK ANALYSIS + ML PREDICTIONS:");
        
        for (Map.Entry<String, HospitalMetrics> entry : hospitalMetrics.entrySet()) {
            String hospital = entry.getKey();
            HospitalMetrics metrics = entry.getValue();
            
            // Traditional outbreak detection
            if (metrics.caseVelocity >= 2) {
                System.out.println("⚠️ OUTBREAK ALERT: " + hospital);
                System.out.println("   📈 Case velocity: " + metrics.caseVelocity + " cases/minute");
                System.out.println("   🌡️ Avg temperature: " + String.format("%.1f°F", metrics.avgTemperature));
                System.out.println("   📋 Symptom diversity: " + (int)metrics.symptomDiversity + " patterns");
                
                // ML Risk Score
                double riskScore = hospitalRiskScores.getOrDefault(hospital, 0.0);
                String riskLevel = getRiskLevel(riskScore);
                System.out.println("   🤖 AI Risk Score: " + String.format("%.1f%%", riskScore) + " (" + riskLevel + ")");
                
                if (riskScore > 75) {
                    System.out.println("   🚨 HIGH AI-PREDICTED OUTBREAK RISK!");
                } else if (riskScore > 50) {
                    System.out.println("   ⚠️ MODERATE AI-PREDICTED RISK");
                }
            }
        }
        
        System.out.println("=" .repeat(70));
    }
    
    private static void updateMLModel() {
        try {
            // Create training instances from current hospital data
            for (Map.Entry<String, HospitalMetrics> entry : hospitalMetrics.entrySet()) {
                HospitalMetrics metrics = entry.getValue();
                
                if (metrics.totalCases >= 3) { // Only use hospitals with enough data
                    // Create training instance
                    double[] values = new double[trainingData.numAttributes()];
                    values[0] = metrics.caseVelocity;
                    values[1] = metrics.avgTemperature;
                    values[2] = metrics.symptomDiversity;
                    values[3] = metrics.severeCaseRatio;
                    
                    // Determine class label based on outbreak indicators
                    String riskClass;
                    if (metrics.hadRecentOutbreak || metrics.caseVelocity > 4 || metrics.avgTemperature > 102) {
                        riskClass = "HIGH";
                    } else if (metrics.caseVelocity > 2 || metrics.avgTemperature > 101) {
                        riskClass = "MEDIUM";
                    } else {
                        riskClass = "LOW";
                    }
                    
                    values[4] = trainingData.attribute(4).indexOfValue(riskClass);
                    
                    Instance instance = new DenseInstance(1.0, values);
                    instance.setDataset(trainingData);
                    trainingData.add(instance);
                }
            }
            
            // Train model if we have enough data
            if (trainingData.numInstances() >= 10 && !modelTrained) {
                System.out.println("🧠 Training ML model with " + trainingData.numInstances() + " examples...");
                outbreakModel.buildClassifier(trainingData);
                modelTrained = true;
                System.out.println("✅ ML Model trained successfully!");
                
                // Show model evaluation
                Evaluation eval = new Evaluation(trainingData);
                eval.evaluateModel(outbreakModel, trainingData);
                System.out.println("📊 Model Accuracy: " + String.format("%.2f%%", eval.pctCorrect()));
            }
            
        } catch (Exception e) {
            System.err.println("Error updating ML model: " + e.getMessage());
        }
    }
    
    private static void generateRiskPredictions() {
        if (!modelTrained) return;
        
        try {
            System.out.println("\n🤖 AI OUTBREAK RISK PREDICTIONS:");
            
            for (Map.Entry<String, HospitalMetrics> entry : hospitalMetrics.entrySet()) {
                String hospital = entry.getKey();
                HospitalMetrics metrics = entry.getValue();
                
                // Create prediction instance
                double[] values = new double[trainingData.numAttributes()];
                values[0] = metrics.caseVelocity;
                values[1] = metrics.avgTemperature;
                values[2] = metrics.symptomDiversity;
                values[3] = metrics.severeCaseRatio;
                values[4] = 0; // placeholder for class
                
                Instance instance = new DenseInstance(1.0, values);
                instance.setDataset(trainingData);
                
                // Get prediction
                double[] distribution = outbreakModel.distributionForInstance(instance);
                double highRiskProb = distribution[2] * 100; // HIGH risk probability
                
                hospitalRiskScores.put(hospital, highRiskProb);
                
                String riskLevel = getRiskLevel(highRiskProb);
                System.out.println("🏥 " + hospital + ": " + String.format("%.1f%%", highRiskProb) + 
                                 " risk (" + riskLevel + ")");
                
                // Feature importance for this hospital
                System.out.println("   📊 Velocity: " + String.format("%.1f", metrics.caseVelocity) +
                                 " | Temp: " + String.format("%.1f°F", metrics.avgTemperature) +
                                 " | Symptoms: " + (int)metrics.symptomDiversity +
                                 " | Severe: " + String.format("%.0f%%", metrics.severeCaseRatio * 100));
            }
            
        } catch (Exception e) {
            System.err.println("Error generating predictions: " + e.getMessage());
        }
    }
    
    private static String getRiskLevel(double riskScore) {
        if (riskScore > 75) return "HIGH";
        else if (riskScore > 50) return "MEDIUM";
        else if (riskScore > 25) return "LOW-MEDIUM";
        else return "LOW";
    }
    
    // Helper method to extract values from JSON-like string
    private static String extractValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start != -1) {
                start += pattern.length();
                int end = json.indexOf("\"", start);
                if (end > start) {
                    return json.substring(start, end);
                }
            }
            
            // For numeric values without quotes
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            if (start != -1) {
                start += pattern.length();
                int end = json.indexOf(",", start);
                if (end == -1) end = json.indexOf("}", start);
                if (end > start) {
                    return json.substring(start, end);
                }
            }
        } catch (Exception e) {
            // Return default
        }
        return "0";
    }
}
