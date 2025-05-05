import java.security.MessageDigest;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Each thread have to find a nonce so that 
 * SHA-256 ("text" + nonce) should start with TARGET_PREFIX
 */

public class Main {
    private static final String BASE_TEXT = "ANOT";
    private static final int MAX_NONCE = Integer.MAX_VALUE;

    public static void main(String[] s) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter TARGET_PREFIX:");
        String TARGET_PREFIX = scanner.nextLine();
        scanner.close();

        if (TARGET_PREFIX == null || TARGET_PREFIX.isEmpty()) {
            System.out.println("Retry again");
            return;
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        //Thread safe
        AtomicBoolean found = new AtomicBoolean(false);

        CompletableFuture<Void>[] tasks = new CompletableFuture[numThreads];

        System.out.println("Working on " + numThreads + " threads");

        long start = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            tasks[i] = CompletableFuture.runAsync(() -> {
                try {
                    for (int nonce = threadIndex; nonce < MAX_NONCE; nonce=nonce + numThreads) {
                        String input = BASE_TEXT + nonce;
                        String hash = sha256(input);
                        if (hash.startsWith(TARGET_PREFIX)) {
                            if (found.compareAndSet(false, true)) {
                                long duration = System.currentTimeMillis() - start;
                                System.out.println("Found nonce! " + nonce);
                                System.out.println("Hash: " + hash);
                                System.out.println("Time: " + duration + " ms");
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, executorService);
        }

        CompletableFuture.allOf(tasks).join();
        executorService.shutdown();
    }    
    
    private static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(("SHA-256"));
        byte[] hashBytes = digest.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

