public class encoder {
        public static void main(String[] args) {
            String password = "omor1234";
            String encodedPassword = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password);
            System.out.println("Encoded password: " + encodedPassword);
        }
}
