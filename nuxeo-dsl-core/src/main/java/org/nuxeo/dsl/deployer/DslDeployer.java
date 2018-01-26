package org.nuxeo.dsl.deployer;

public interface DslDeployer {
    void deployDsl(String dsl);
    String getDsl();
}
