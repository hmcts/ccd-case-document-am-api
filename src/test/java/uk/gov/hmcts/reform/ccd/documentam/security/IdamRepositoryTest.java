package uk.gov.hmcts.reform.ccd.documentam.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IdamRepositoryTest {

    private static final String TEST_BEAR_TOKEN = "TestBearToken";

    @Mock
    private IdamClient idamClient;

    @InjectMocks
    private IdamRepository idamRepository;

    @Test
    @DisplayName("Get user info if token is passed")
    void shouldGetUserInfo() {
        UserInfo userInfo = UserInfo.builder().build();
        given(idamClient.getUserInfo(TEST_BEAR_TOKEN)).willReturn(userInfo);
        UserInfo result = idamRepository.getUserInfo(TEST_BEAR_TOKEN);
        assertThat(result).isSameAs(userInfo);
    }

}

