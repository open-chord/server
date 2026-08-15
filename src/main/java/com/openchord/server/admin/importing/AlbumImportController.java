package com.openchord.server.admin.importing;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** HTTP API for the reviewable two-phase album import workflow. */
@RestController
@RequestMapping("/api/admin/imports")
public class AlbumImportController {
    private final AlbumImportService imports;

    public AlbumImportController(AlbumImportService imports) {
        this.imports = imports;
    }

    @PostMapping(path = "/analyze", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public AlbumImportDraft analyze(@RequestParam List<MultipartFile> files)
            throws IOException, InterruptedException {
        return imports.analyze(files);
    }

    @PostMapping("/{id}/commit")
    @ResponseStatus(HttpStatus.CREATED)
    public AlbumImportResult commit(
            @PathVariable UUID id, @RequestBody CommitAlbumImport request)
            throws IOException, InterruptedException {
        return imports.commit(id, request);
    }
}
