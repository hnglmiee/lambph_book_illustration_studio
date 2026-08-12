package com.hoanglam.bis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileServeController {

    @GetMapping("/portrait/{characterId}")
    public ResponseEntity<Resource> getPortrait(@PathVariable UUID characterId) throws Exception {
        return serveImage(Paths.get("./data/images"), characterId);
    }

    @GetMapping("/illustration/{chapterId}")
    public ResponseEntity<Resource> getIllustration(@PathVariable UUID chapterId) throws Exception {
        return serveImage(Paths.get("./data/images"), chapterId);
    }

    private ResponseEntity<Resource> serveImage(Path dir, UUID id) throws Exception {
        Path pngPath = dir.resolve(id + ".png");
        Path jpgPath = dir.resolve(id + ".jpg");
        Path target = Files.exists(pngPath) ? pngPath : jpgPath;

        if (!Files.exists(target)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = target.toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(target));
    }
}