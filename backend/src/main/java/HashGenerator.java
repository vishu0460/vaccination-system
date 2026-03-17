import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = "Vaccine@#6030";
        String hashed = encoder.encode(password);
        System.out.println("BCrypt12 hash for '" + password + "': " + hashed);
    }
}

