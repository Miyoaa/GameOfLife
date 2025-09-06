package it.polito.extgol.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Assume;

public class TestBranchUtils {

    private static String currentBranch;
    
    static {
        currentBranch = System.getenv("CI_COMMIT_REF_NAME");
        if(currentBranch==null) // not in CI!
        try {
            String head = Files.readString(Path.of(".git/HEAD"));
            if( head.startsWith("ref:")){
                // branch checked-out
                currentBranch = head.substring(head.lastIndexOf("/") + 1).trim();
            }else{ 
                currentBranch = "main"; // in case of detached HEAD behave like main branch
            }
        } catch (IOException e) {
            // in case branch cannot be determined, assume `main`
            currentBranch = "main";
        }
    }

    /**
     * Checks whether the current branch is one that is 
     * matches the specified requirements. If that is the case
     * or the current branch is the {@code main} branch,
     * the execution proceeds; otherwise the assumption is not
     * satisfied and therefore the test that invoked this method will be
     * skipped.
     * <p>
     * The matching is performed on the whole word.
     * For instance in GitLab when a branch is generated from and issue
     * with title {@code "R1 Arithmentic operations"} the default name
     * is {@code 1-r1-arithmetic-operations}, therefore using the statement
     * {@code assumeBranch("r1")} in a test will execute the test only in
     * the corresponding branch or in the {@code main} branch.
     * 
     * @param rs the keywors to be matched in the current branch name
     */
    public static void assumeBranch(String... rs){
        Assume.assumeTrue("Skipping since not in any of " + Arrays.toString(rs) + " branches", 
                          currentBranch.equals("main") || currentBranch.equals("exam-lab-test") ||
                          Arrays.stream(rs)
                          .allMatch(r -> currentBranch.toLowerCase().contains("-" + r.toLowerCase()) ));
    }

}
