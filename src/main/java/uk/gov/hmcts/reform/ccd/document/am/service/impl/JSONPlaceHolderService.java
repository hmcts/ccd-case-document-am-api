package uk.gov.hmcts.reform.ccd.document.am.service.impl;


import java.util.List;

import uk.gov.hmcts.reform.ccd.document.am.model.Post;

public interface JSONPlaceHolderService {

    List<Post> getPosts();

    Post getPostById(Long id);
}
