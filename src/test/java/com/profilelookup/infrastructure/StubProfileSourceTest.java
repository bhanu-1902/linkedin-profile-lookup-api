package com.profilelookup.infrastructure;

import com.profilelookup.domain.ProfileSource;
import com.profilelookup.infrastructure.stub.StubProfileSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubProfileSourceTest extends ProfileSourceContractTest {

    private final StubProfileSource stub = new StubProfileSource();

    @Override
    protected ProfileSource source() {
        return stub;
    }

    @Test
    void alwaysReturnsEmptyRegardlessOfUrl() {
        assertThat(stub.findByUrl("https://www.linkedin.com/in/anyone-at-all")).isEmpty();
    }
}
