package com.spreadsheet_parser.controller;

import com.spreadsheet_parser.service.aws.s3.S3PresignerService;
import com.spreadsheet_parser.service.aws.s3.S3Service;
import com.spreadsheet_parser.service.aws.s3.model.CompleteMultipartRequest;
import com.spreadsheet_parser.service.aws.s3.model.MultipartUploadResponse;
import com.spreadsheet_parser.task.CompanyMatchTaskRunner;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.Map;

@RestController
@RequestMapping("/company")
public class CompanyResource {

    private final CompanyMatchTaskRunner taskRunner;
    private final S3Service s3Service;
    private final S3PresignerService s3PresignerService;

    @Inject
    CompanyResource(
            CompanyMatchTaskRunner taskRunner,
            S3Service s3Service,
            S3PresignerService s3PresignerService
    ){
        this.taskRunner = taskRunner;
        this.s3Service = s3Service;
        this.s3PresignerService = s3PresignerService;
    }

    @GetMapping()
    ModelAndView getCompanyResource() {
        return new ModelAndView("pages/company/company.html");
    }

    @PostMapping(value = "/init-multipart-upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public MultipartUploadResponse initMultipartUpload(@RequestBody Map<String, String> request){

        String filename = request.get("filename");
        String uploadId = s3Service.generateUploadId(filename);

        MultipartUploadResponse response = new MultipartUploadResponse();
        response.setUploadId(uploadId);
        response.setPresignedUrls(s3PresignerService.generateUploadUrls(uploadId, Integer.parseInt(request.get("partCount")), filename));

        return response;

    }

    @PostMapping(value = "/process-csv", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> getRowBatchFromCsv(@RequestBody Map<String, String> request) throws InterruptedException {
        new Thread(() -> taskRunner.runTask(request.get("key"), Integer.parseInt(request.get("totalRows")), request.get("email"))).start();
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/complete-multipart-upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> completeMultipartUpload(@RequestBody CompleteMultipartRequest request){
        s3Service.completeMultipartUpload(request);
        return ResponseEntity.noContent().build();
    }

}
