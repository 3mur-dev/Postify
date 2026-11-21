package com.omar.postify.service.storage;

import com.omar.postify.dto.StorageMediaResult;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostMedia;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface FileStorageService {

    StorageMediaResult storeMedia(MultipartFile media);

    Resource loadPostMedia(PostMedia media);

    String storeAvatar(MultipartFile avatar);

    void deletePostMediaIfExists(Post post);

    void deleteAvatarIfExists(String avatarUrl);

    String getAnalysisThumbnailUrl(PostMedia media);
}