import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        ProcessBuilder ports = new ProcessBuilder("netstat", "-an");
        ProcessBuilder firewall = new ProcessBuilder("netsh", "advfirewall", "show", "allprofiles", "state");
        ProcessBuilder fileper = new ProcessBuilder("icacls", "C:\\dev\\audit-test\\private-key.txt");

        // ===== FIREWALL CHECK =====
        Process processFirewall = firewall.start();
        List<String> results = new ArrayList<>();   // declared OUTSIDE the try, so we can use it after

        try (BufferedReader readerFirewall = new BufferedReader(
                new InputStreamReader(processFirewall.getInputStream()))) {
            String currentProfile = "";              // remembers the last profile seen
            String line;

            while ((line = readerFirewall.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.endsWith("Profile Settings:")) {     // e.g. "Public Profile Settings:"
                    String[] words = trimmed.split("\\s+");
                    currentProfile = words[0];                    // "Public"
                }

                if (trimmed.startsWith("State")) {               // e.g. "State      ON"
                    String[] words = trimmed.split("\\s+");
                    String lastWord = words[words.length - 1];    // "ON" or "OFF"
                    results.add(currentProfile + ": " + lastWord);
                }
            }
        }
        processFirewall.waitFor();

        System.out.println(results);


        // PART 1 — collect the profiles that are OFF
        List<String> offProfiles = new ArrayList<>();
        for (String result : results) {
            if (result.endsWith("OFF")) {
                offProfiles.add(result.split(":")[0]);   // "Public"
            }
        }

        // PART 2 — decide
        if (results.size() != 3) {
            System.out.println("Firewall: ERROR - could not read firewall state (found "
                    + results.size() + " of 3 profiles)");
        } else if (!offProfiles.isEmpty()) {
            System.out.println("Firewall: WARNING - profile(s) off: " + String.join(", ", offProfiles));
        } else {
            System.out.println("Firewall: OK - all profiles active (Domain, Private, Public)");
        }
    }
}