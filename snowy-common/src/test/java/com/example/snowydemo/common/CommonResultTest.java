package com.example.snowydemo.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonResultTest {

    @Test
    void dataWrapsPayloadWithSuccessCodeAndMessage() {
        CommonResult<String> result = CommonResult.data("hello");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    void createsHttpAlignedResponseBodies() {
        CommonResult<String> created = CommonResult.created("customer");
        CommonResult<Void> denied = CommonResult.error(403, "没有操作权限");

        assertThat(created.getCode()).isEqualTo(201);
        assertThat(created.getMessage()).isEqualTo("created");
        assertThat(created.getData()).isEqualTo("customer");
        assertThat(denied.getCode()).isEqualTo(403);
        assertThat(denied.getMessage()).isEqualTo("没有操作权限");
        assertThat(denied.getData()).isNull();
    }
}
