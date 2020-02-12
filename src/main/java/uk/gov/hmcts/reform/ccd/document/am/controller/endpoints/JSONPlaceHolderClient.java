package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.gov.hmcts.reform.ccd.document.am.configuration.FeignClientConfiguration;
import uk.gov.hmcts.reform.ccd.document.am.hystrix.JSONPlaceHolderFallback;
import uk.gov.hmcts.reform.ccd.document.am.model.Post;

import java.util.List;

@FeignClient(value = "jplaceholder",
             url = "https://jsonplaceholder.typicode.com/",
             configuration = FeignClientConfiguration.class,
             fallback = JSONPlaceHolderFallback.class)
public interface JSONPlaceHolderClient {

    @RequestMapping(method = RequestMethod.GET, value = "/posts")
    List<Post> getPosts();


    @RequestMapping(method = RequestMethod.GET, value = "/posts/{postId}", produces = "application/json")
    Post getPostById(@PathVariable("postId") Long postId);
}
