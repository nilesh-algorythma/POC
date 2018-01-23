package com.vxg.cloudsdk.suite;

import com.vxg.cloudsdk.CloudTrialConnectionTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs all unit tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({CloudTrialConnectionTest.class})
public class UnitTestSuite {}