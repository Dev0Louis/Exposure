package io.github.mortuusars.exposure.test;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.test.framework.Test;
import io.github.mortuusars.exposure.test.framework.TestResult;
import io.github.mortuusars.exposure.test.framework.TestingResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.server.network.ServerPlayerEntity;

public class Tests {
    private final ServerPlayerEntity player;

    public Tests(ServerPlayerEntity player) {
        this.player = player;
    }

    public TestingResult run() {
        LogUtils.getLogger().info("RUNNING TESTS");

        Pair<List<TestResult>, List<TestResult>> ran = run(
                List.of()
                //new ExposurePredicateTests().collect()
        );

        List<TestResult> skipped = skip();
        TestingResult testingResult = new TestingResult(ran.getFirst(), ran.getSecond(), skipped);
        LogUtils.getLogger().info(String.join("",
                "TESTS COMPLETED!\n",
                testingResult.getTotalTestCount() + " test(s) were conducted.",
                testingResult.passed().size() > 0 ? ("\nPassed:\n" + testingResult.passed().stream()
                        .map(TestResult::toString).collect(Collectors.joining("\n"))) : "",
                testingResult.failed().size() > 0 ? ("\nFailed:\n" + testingResult.failed().stream()
                        .map(TestResult::toString).collect(Collectors.joining("\n"))) : "",
                testingResult.skipped().size() > 0 ? ("\nSkipped:\n" + testingResult.skipped().stream()
                        .map(TestResult::toString).collect(Collectors.joining("\n"))) : ""));
        return testingResult;
    }

    @SafeVarargs
    private Pair<List<TestResult>, List<TestResult>> run(List<Test>... tests) {
        List<TestResult> passed = new ArrayList<>();
        List<TestResult> failed = new ArrayList<>();
        for (List<Test> list : tests) {
            for (Test test : list) {
                TestResult testResult = runTest(test);
                if (testResult.status() == TestResult.Status.PASSED)
                    passed.add(testResult);
                else
                    failed.add(testResult);
            }
        }
        return Pair.of(passed, failed);
    }

    @SafeVarargs
    private List<TestResult> skip(List<Test>... tests) {
        List<TestResult> results = new ArrayList<>();
        for (List<Test> list : tests) {
            for (Test test : list) {
                results.add(TestResult.skip(test.name));
            }
        }
        return results;
    }

    private TestResult runTest(Test test) {
        try {
            test.test.accept(player);
            return TestResult.pass(test.name);
        }
        catch (Exception e) {
            return TestResult.error(test.name, e.toString());
        }
    }
}
