package com.minus.git.reactive.backend

import org.springframework.http.MediaType

class GitConst {
    enum class ContentType(val mediaType: MediaType) {
        X_GIT_LOOSE_OBJECT(MediaType("application", "x-git-loose-object")),
        X_GIT_PACKED_OBJECTS(MediaType("application", "x-git-packed-objects")),
        X_GIT_PACKED_OBJECTS_TOC(MediaType("application", "x-git-packed-objects-toc")),
        X_GIT_UPLOAD_PACK_RESULT(MediaType("application", "x-git-upload-pack-result")),
        X_GIT_RECEIVE_PACK_RESULT(MediaType("application", "x-git-receive-pack-result")),
        X_GIT_UPLOAD_PACK_ADVERTISEMENT(MediaType("application", "x-git-upload-pack-advertisement")),
        X_GIT_RECEIVE_PACK_ADVERTISEMENT(MediaType("application", "x-git-receive-pack-advertisement"))
    }

    enum class Cmd(private val contentType: ContentType, private val responseContentType: ContentType) {
        GIT_UPLOAD_PACK(ContentType.X_GIT_UPLOAD_PACK_ADVERTISEMENT, ContentType.X_GIT_UPLOAD_PACK_RESULT),
        GIT_RECEIVE_PACK(ContentType.X_GIT_RECEIVE_PACK_ADVERTISEMENT, ContentType.X_GIT_RECEIVE_PACK_RESULT);

        val slug: String
            get() = name.lowercase().replace('_', '-')

        fun match(raw: String) = valueOf(raw.uppercase())

        val cfgName: String
            get() = name.lowercase()

        val mediaType: MediaType
            get() = contentType.mediaType

        val responseMediaType: MediaType
            get() = responseContentType.mediaType
    }
}
