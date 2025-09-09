import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class HealthcareProducer {
    private static final String TOPIC_NAME = "patient-reports";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    
    // Hospital locations for outbreak simulation
    private static final String[] HOSPITALS = {
        "Central General Hospital",
        "St. Mary's Medical Center", 
        "Downtown Emergency Clinic",
        "North Side Hospital",
        "Regional Medical Center"
    };
    
    // Respiratory symptoms for pattern detection
    private static final String[] SYMPTOMS = {
        "fever,cough,fatigue",
        "fever,shortness_of_breath,chest_pain",
        "cough,sore_throat,headache",
        "fever,cough,shortness_of_breath",
        "fatigue,body_aches,fever",
        "dry_cough,fever,loss_of_taste",
        "severe_cough,high_fever,breathing_difficulty"
    };
    
    private static final String[] SEVERITY_LEVELS = {"mild", "moderate", "severe", "critical"};
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        Producer<String, String> producer = new KafkaProducer<>(props);
        Random random = new Random();
        
        System.out.println("Starting Healthcare Data Stream...");
        System.out.println("Simulating respiratory illness patterns across hospitals");
        
        try {
            for (int i = 0; i < 1000; i++) {
                // Generate patient data
                String patientId = "PATIENT_" + (1000 + i);
                String hospital = HOSPITALS[random.nextInt(HOSPITALS.length)];
                String symptoms = SYMPTOMS[random.nextInt(SYMPTOMS.length)];
                String severity = SEVERITY_LEVELS[random.nextInt(SEVERITY_LEVELS.length)];
                double temperature = 98.6 + (random.nextGaussian() * 2) + (severity.equals("severe") ? 3 : 0);
                long timestamp = System.currentTimeMillis();
                
                // Create JSON-like message
                String patientReport = String.format(
                    "{\"patientId\":\"%s\",\"hospital\":\"%s\",\"symptoms\":\"%s\",\"severity\":\"%s\",\"temperature\":%.1f,\"timestamp\":%d}",
                    patientId, hospital, symptoms, severity, temperature, timestamp
                );
                
                // Send to Kafka
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, patientId, patientReport);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        System.err.println("Error sending message: " + exception.getMessage());
                    } else {
                        System.out.println("Sent: " + patientReport);
                    }
                });
                
                // Simulate different outbreak patterns
                if (i % 50 == 0 && i > 0) {
                    System.out.println("\n--- Potential outbreak spike detected ---");
                    // Send cluster of similar cases from same hospital
                    String outbreakHospital = HOSPITALS[random.nextInt(HOSPITALS.length)];
                    String outbreakSymptoms = SYMPTOMS[random.nextInt(SYMPTOMS.length)];
                    
                    for (int j = 0; j < 5; j++) {
                        String clusterId = "CLUSTER_" + i + "_" + j;
                        String clusterReport = String.format(
                            "{\"patientId\":\"%s\",\"hospital\":\"%s\",\"symptoms\":\"%s\",\"severity\":\"%s\",\"temperature\":%.1f,\"timestamp\":%d,\"cluster\":true}",
                            clusterId, outbreakHospital, outbreakSymptoms, "moderate", 101.0 + random.nextDouble(), System.currentTimeMillis()
                        );
                        producer.send(new ProducerRecord<>(TOPIC_NAME, clusterId, clusterReport));
                        TimeUnit.MILLISECONDS.sleep(100);
                    }
                }
                
                // Wait between normal reports
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException e) {
            System.out.println("Data generation interrupted");
        } finally {
            producer.close();
            System.out.println("Healthcare data stream completed");
        }
    }
}
