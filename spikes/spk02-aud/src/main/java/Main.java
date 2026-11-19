import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Main {
    public static void main(String[] args) throws Exception {
        String keyFilePath="C:\\dev\\audit-test\\private-key.txt";
        ProcessBuilder ports = new ProcessBuilder("netstat", "-an");
        ProcessBuilder firewall = new ProcessBuilder("netsh", "advfirewall", "show", "allprofiles", "state");
        ProcessBuilder fileper = new ProcessBuilder("icacls", keyFilePath);

        // ===== FIREWALL CHECK =====
        Process processFirewall = firewall.start();
        List<String> results = new ArrayList<>();   // declared OUTSIDE the try, so we can use it after

        try (BufferedReader readerFirewall = new BufferedReader(new InputStreamReader(processFirewall.getInputStream()))) {
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



        // PART 1 — collect the profiles that are OFF
        List<String> offProfiles = new ArrayList<>();
        for (String result : results) {
            if (result.endsWith("OFF")) {
                offProfiles.add(result.split(":")[0]);   // "Public"
            }
        }

        // PART 2 — decide
        if (results.size() != 3) {
            System.out.println("Firewall: ERROR - could not read firewall state (found " + results.size() + " of 3 profiles)");
        } else if (!offProfiles.isEmpty()) {
            System.out.println("Firewall: WARNING - profile(s) off: " + String.join(", ", offProfiles));
        } else {
            System.out.println("Firewall: OK - all profiles active (Domain, Private, Public)");
        }

        Process processPorts = ports.start();
        Set<String> openPorts = new TreeSet<>();
        List<String> notAllowed = new ArrayList<>();
        List<String> allowed = new ArrayList<>();
        List<String> allowedPorts = List.of("443", "80", "8080");

        try (BufferedReader readerPorts = new BufferedReader(new InputStreamReader(processPorts.getInputStream()))) {
            String line;

            while ((line = readerPorts.readLine()) != null) {

                String trimmed = line.trim();
                if (trimmed.contains("LISTENING")) {
                    String[] words = trimmed.split("\\s+");
                    String address = words[1];
                    int pos = address.lastIndexOf(":");
                    if (!address.startsWith("127.") && !address.startsWith("[::1]")) {
                        String port = address.substring(pos + 1);
                        openPorts.add(port);
                    }
                }

            }
            if (openPorts.isEmpty()) {
                System.out.println("Open ports: ERROR - could not read netstat output (no LISTENING lines found)");
            } else {
                for (String port : openPorts) {
                    if (allowedPorts.contains(port)) {
                        allowed.add(port);
                    } else {
                        notAllowed.add(port);
                    }
                }
                if (notAllowed.isEmpty()) {
                    System.out.println("Open ports: OK - open and allowed: " + String.join(", ", allowed));
                } else {
                    System.out.println("Open ports: WARNING - unexpected ports listening: " + String.join(", ", notAllowed));
                }

            }

        }
        processPorts.waitFor();

        Process processFilePer = fileper.start();
        List<String> NotAllowedGroups = new ArrayList<>();
        List<String> allowedGroups = new ArrayList<>();

        try (BufferedReader readerfileper = new BufferedReader(new InputStreamReader(processFilePer.getInputStream()))) {
            String line;

            while ((line = readerfileper.readLine()) != null) {
                String trimmed = line.trim();
                int pos=trimmed.lastIndexOf(":");
                if (pos == -1) continue;
                String permission = trimmed.substring(0, pos);
                if(permission.endsWith("Administrators") || permission.endsWith("SYSTEM") ){
                    String name = permission.substring(permission.lastIndexOf("\\") + 1);
                    allowedGroups.add(name);

                }else{
                    String name = permission.substring(permission.lastIndexOf("\\") + 1);
                    NotAllowedGroups.add(name);
                }

            }
            if(NotAllowedGroups.isEmpty() && allowedGroups.isEmpty()){
                System.out.println("File permissions: ERROR - file not found: "+keyFilePath);
            }else{
                if(!NotAllowedGroups.isEmpty()){
                    System.out.println("File permissions: WARNING - too broad access: "+String.join(", ", NotAllowedGroups));
                }else{
                    System.out.println("File permissions: OK - only SYSTEM and Administrators have access");
                }
            }

        }
        processFilePer.waitFor();
    }


}