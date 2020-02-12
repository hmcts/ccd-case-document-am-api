package uk.gov.hmcts.reform.ccd.document.am.hystrix;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.document.am.controller.endpoints.JSONPlaceHolderClient;
import uk.gov.hmcts.reform.ccd.document.am.model.Post;

import java.util.Collections;
import java.util.List;

@Component
public class JSONPlaceHolderFallback implements JSONPlaceHolderClient {

    @Override
    public List<Post> getPosts() {
        return Collections.emptyList();
    }

    @Override
    public Post getPostById(Long postId) {
        return null;
    }
}
