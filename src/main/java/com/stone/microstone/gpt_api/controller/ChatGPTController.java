package com.stone.microstone.gpt_api.controller;

import com.stone.microstone.WorkBook;
import com.stone.microstone.gpt_api.dto.QuestionAnswerResponse;
import com.stone.microstone.gpt_api.service.ChatGPTService;
import com.stone.microstone.workbook.*;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jdbc.Work;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping(value = "/api/workbook")
public class ChatGPTController {

    private final ChatGPTService chatGPTService;
    private final WorkBookService workBookService;
    private final PdfService pdfService;
    private final WorkBookRepository workBookRepository;
    private final HttpSession httpSession;

    public ChatGPTController(ChatGPTService chatGPTService, WorkBookService workBookService,
                             PdfService pdfService,WorkBookRepository workBookRepository,
                             HttpSession httpSession) {
        this.chatGPTService = chatGPTService;
        this.workBookService = workBookService;
        this.pdfService = pdfService;
        this.workBookRepository = workBookRepository;
        this.httpSession = httpSession;
    }

    @PostMapping("/processText")
    public ResponseEntity<Map<String, Object>> processText(@RequestBody String problemText) {
        log.debug("받은 문제 텍스트: " + problemText);
       // Integer userId = 8;

        Integer userId = (Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }

        // 1단계: 문제 텍스트 요약
        Map<String, Object> summaryResult = chatGPTService.summarizeText(problemText);

        // 요약된 텍스트 추출
        String summarizedText = (String) summaryResult.get("content");

        if (summarizedText == null || summarizedText.trim().isEmpty()) {
            log.error("요약된 텍스트가 없습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "요약된 텍스트가 없습니다."));
        }

        log.debug("요약된 텍스트: " + summarizedText);

        // 2단계: 요약된 텍스트로 문제 생성
        Map<String, Object> questionResult = chatGPTService.generateQuestion(summarizedText);

        String Question = (String) questionResult.get("content");
        try {

            log.debug("생성된 질문: " + Question);

            // 3단계: 질문으로 답변 생성
            Map<String, Object> answerResult = chatGPTService.generateAnswer(Question);
            String answerText = (String) answerResult.get("content");
            WorkBook saveWorkBook = workBookService.findAndsaveWorkBook(Question, summarizedText,answerText, userId);

            if (answerText == null || answerText.trim().isEmpty()) {
                log.error("생성된 답변이 없습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "생성된 답변이 없습니다."));
            }
            pdfService.save(saveWorkBook.getWb_user_id(),userId);
            pdfService.answersave(saveWorkBook.getWb_user_id(),userId);
            log.debug("생성된 답변: " + answerText);
            QuestionAnswerResponse response=new QuestionAnswerResponse(saveWorkBook.getWb_user_id(),saveWorkBook.getWb_title(),Question,answerText);

            // 결과 반환
            return new ResponseEntity<>(Map.of("message",response), HttpStatus.OK);


        } catch (Exception e) {
            log.error("오류발생", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "WorkBook 저장 중 오류가 발생했습니다." + e.getMessage()));
        }

    }


    @PostMapping("/retext")
    public ResponseEntity<Object> retext(){
//        Integer userId = 8;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook lastWorkBook=workBookService.findWorkBook(userId);
            Map<String,Object> questionResult = chatGPTService.regenerateQuestion(lastWorkBook.getWb_sumtext(), lastWorkBook.getWb_content()); //저장해둔 요약문자로 다시 생성.
            String newQuestion=(String) questionResult.get("content");
            log.debug("새 질문={}",newQuestion);

             //다음에 조회할때 필요할수 있으니 작성.

            Map<String, Object> answerResult = chatGPTService.generateAnswer(newQuestion);
            String answerText = (String) answerResult.get("content");
            WorkBook saveWorkBook = workBookService.findLastWorkBook(newQuestion,answerText, userId);

            if (answerText == null || answerText.trim().isEmpty()) {
                log.error("생성된 답변이 없습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "생성된 답변이 없습니다."));
            }

            log.debug("생성된 답변: " + answerText);
            QuestionAnswerResponse response=new QuestionAnswerResponse(saveWorkBook.getWb_user_id(),saveWorkBook.getWb_title(),newQuestion,answerText);

            // 결과 반환
            return new ResponseEntity<>(Map.of("message",response), HttpStatus.OK);
            // 결과 반환
            //return new ResponseEntity<>(Map.of("question", newQuestion, "answer", answerText, "workBook", saveWorkBook.getWb_user_id()), HttpStatus.OK);

        }catch(Exception e){
            log.error("오류발생");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "WorkBook 저장 중 오류가 발생했습니다."+e.getMessage()));
        }
    }

    @PatchMapping("/favorite")
    public ResponseEntity<Object> favorite(@RequestParam Integer wb_id){
   //     Integer userId = 6;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook workBook=workBookService.findFavorite(wb_id,userId);
            return new ResponseEntity<>(Map.of("message","즐겨찾기가 완료되었습니다","wb_id", workBook.getWb_user_id(),"favorite",workBook.isWb_favorite()), HttpStatus.OK);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/answer/favorite")
    public ResponseEntity<Object> favoriteanswer(@RequestParam Integer wb_id){
        //     Integer userId = 6;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook workBook=workBookService.findAnswerFavorite(wb_id,userId);
            return new ResponseEntity<>(Map.of("message","즐겨찾기가 완료되었습니다","wb_id", workBook.getWb_user_id(),"favorite",workBook.isWb_favorite_answer()), HttpStatus.OK);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity bookall(){
//        Integer userId = 7;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            List<WorkBook> allbook=workBookService.findWorkBookall(userId);
            if(allbook.isEmpty()){
                return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
            }
            List<Map<String, Object>> pdfworkbook = pdfService.getPdfsForWorkbook(allbook);
            List<Map<String, Object>> completeData = allbook.stream()
                    .map(book -> {
                        Map<String, Object> pdfData = pdfworkbook.stream()
                                .filter(pdf -> pdf.get("wb_id").equals(book.getWb_id()))
                                .findFirst()
                                .orElse(Map.of("workbook_pdf", Map.of("filename", "", "pdf_path", "")));

                        return Map.of(
                                "wb_id", Optional.ofNullable(book.getWb_user_id()).orElse(-1),
                                "wb_title", Optional.ofNullable(book.getWb_title()).orElse(""),
                                "wb_create", Optional.ofNullable(book.getWb_create()).orElse(LocalDate.now()),
                                "wb_content", Optional.ofNullable(book.getWb_content()).orElse(""),
                                "favorite", book.isWb_favorite(),
                                "workbook_pdf", pdfData.get("workbook_pdf")
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("data", completeData));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    @GetMapping("/favorite/all")
    public ResponseEntity favoriteall(){
//        Integer userId = 5;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            List<WorkBook> allbook=workBookService.findfavoriteWorkBookall(userId);
            List<Map<String, Object>> answers = allbook.stream()
                    .map(book -> Map.of("wb_id",(Object) book.getWb_user_id(),"wb_title",book.getWb_title()
                            ,"wb_create",book.getWb_create(), "wb_content", book.getWb_content(),
                            "favorite",book.isWb_favorite()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("data",answers));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity selectbook(@RequestParam Integer wb_id){
//        Integer userId = 8;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook workBook=workBookService.findSearch(wb_id,userId);

            WorkBookPDF workBookPDF=pdfService.findByworkBook(workBook);
            Map<String,Object> workbookMap=Map.of(
                    "pdf_file",workBookPDF.getPdf_path() != null?workBookPDF.getPdf_path():"",
                    "filename",workBookPDF.getFileName() !=null?workBookPDF.getFileName():""
            );
            return ResponseEntity.ok(Map.of("wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title(),"wb_create",workBook.getWb_create(),
                    "content",workBook.getWb_content()
                    ,"workbook_pdf",workbookMap,"favorite",workBook.isWb_favorite()));
            //return ResponseEntity.ok(Map.of("wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title(),"wb_create",workBook.getWb_create(),"content",workBook.getWb_content(),"favorite",workBook.isWb_favorite()));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/answer/all")
    public ResponseEntity answer(){
//        Integer userId = 7;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            List<WorkBook> allbook=workBookService.findWorkBookall(userId);
            if(allbook.isEmpty()){
                return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
            }
            List<Map<String, Object>> pdfanswer = pdfService.getPdfsForanswer(allbook);
            List<Map<String, Object>> completeData = allbook.stream()
                    .map(book -> {
                        Map<String, Object> pdfData = pdfanswer.stream()
                                .filter(pdf -> pdf.get("wb_id").equals(book.getWb_id()))
                                .findFirst()
                                .orElse(Map.of("workbook_pdf", Map.of("filename", "", "pdf_path", "")));

                        return Map.of(
                                "wb_id", Optional.ofNullable(book.getWb_user_id()).orElse(-1),
                                "wb_title", Optional.ofNullable(book.getWb_title_answer()).orElse(""),
                                "wb_create", Optional.ofNullable(book.getWb_create()).orElse(LocalDate.now()),
                                "wb_answer", Optional.ofNullable(book.getWb_answer()).orElse(""),
                                "favorite", book.isWb_favorite_answer(),
                                "workbook_pdf", pdfData.get("workbook_pdf")
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("data", completeData));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/favorite/answer/all")
    public ResponseEntity answerfavorite(){
//        Integer userId = 6;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            List<WorkBook> allbook=workBookService.findfavoriteWorkBookall(userId);
            List<Map<String, Object>> answers = allbook.stream()
                    .map(book -> Map.of("wb_id",(Object) book.getWb_user_id(),"wb_title",book.getWb_title(), "wb_create",book.getWb_create(),"wb_answer", book.getWb_answer(),"favorite",book.isWb_favorite()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("data",answers));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/answer/search")
    public ResponseEntity selectanswer(@RequestParam Integer wb_id){
//        Integer userId = 8;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook workBook=workBookService.findSearch(wb_id,userId);
            AnswerPDF answerPDF=pdfService.anfindByworkBook(workBook);
            Map<String,Object> workbookMap=Map.of(
                    "pdf_file",answerPDF.getPdf_path() != null?answerPDF.getPdf_path():"",
                    "filename",answerPDF.getFileName() !=null?answerPDF.getFileName():""
            );
            return ResponseEntity.ok(Map.of("wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title_answer(),"wb_create",workBook.getWb_create(),
                    "wb_answer",workBook.getWb_answer(),"workbook_pdf",workbookMap,"favorite",workBook.isWb_favorite_answer()));
            //return ResponseEntity.ok(Map.of("wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title(),"wb_create",workBook.getWb_create(),"wb_answer",workBook.getWb_answer(),"favorite",workBook.isWb_favorite()));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/title")
    public ResponseEntity settingtitle(@RequestParam Integer wb_id,@RequestParam String title){
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook workBook=workBookService.findSearchAndtitle(wb_id,userId,title);
            return ResponseEntity.ok(Map.of("message","제목변경 완료","wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title()));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/answer/title")
    public ResponseEntity settinganswertitle(@RequestParam Integer wb_id,@RequestParam String title){
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook workBook=workBookService.findSearchAndanswertitle(wb_id,userId,title);
            return ResponseEntity.ok(Map.of("message","제목변경 완료","wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title_answer()));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload2")
    public ResponseEntity uploadWorkbook(@RequestParam Integer wb_id, @RequestParam("file")MultipartFile file){
//        Integer userId=6;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            pdfService.savedata(file,wb_id,userId);
            return ResponseEntity.ok(Map.of("message","저장완료"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    @PostMapping("/answer/upload2")
    public ResponseEntity uploadanswer(@RequestParam Integer wb_id, @RequestParam("file")MultipartFile file){
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            pdfService.answersavedata(file,wb_id,userId);
            return ResponseEntity.ok(Map.of("message","저장완료"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/upload")
    public ResponseEntity uploadWorkbook2(@RequestParam Integer wb_id, @RequestParam("file")MultipartFile file){
//        Integer userId=6;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            pdfService.savedata2(file,wb_id,userId);
            return ResponseEntity.ok(Map.of("message","저장완료"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    @PostMapping("/answer/upload")
    public ResponseEntity uploadanswer2(@RequestParam Integer wb_id, @RequestParam("file")MultipartFile file){
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            pdfService.answersavedata2(file,wb_id,userId);
            return ResponseEntity.ok(Map.of("message","저장완료"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }





    @DeleteMapping("/delete")
    public ResponseEntity workbookdelete(@RequestParam Integer wb_id){
//        Integer userId=5;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            workBookService.deleteSearch(wb_id,userId);
            return ResponseEntity.ok(Map.of("message","삭제완료"));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/download/{wb_id}")
    public ResponseEntity downloadFile(@PathVariable Integer wb_id) {
//        Integer userId=6;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook workBook=workBookService.findSearch(wb_id,userId);
            WorkBookPDF workBookPDF=pdfService.findByworkBook(workBook);
            String pdfPath=workBookPDF.getPdf_path();
            Path filePath= Paths.get(pdfPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new FileNotFoundException("파일을 찾을수 없음: " + pdfPath);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=\""+resource.getFilename()+"\"")
                            .body(resource);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/answer/download/{wb_id}")
    public ResponseEntity downloadFilean(@PathVariable Integer wb_id) {
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            WorkBook workBook=workBookService.findSearch(wb_id,userId);
            AnswerPDF answerPDF=pdfService.anfindByworkBook(workBook);
            String pdfPath=answerPDF.getPdf_path();
            Path filePath= Paths.get(pdfPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new FileNotFoundException("파일을 찾을수 없음: " + pdfPath);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=\""+resource.getFilename()+"\"")
                    .body(resource);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/download/all")
    public ResponseEntity downloadFileall() {
//        Integer userId=6;
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            List<WorkBook> allbook=workBookService.findWorkBookall(userId);
            List<Map<String, Object>> workBookPDF=pdfService.getPdfsForWorkbook(allbook);

            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
            try(ZipOutputStream zipOutputStream=new ZipOutputStream(byteArrayOutputStream)){
                for(Map<String, Object> pdfMap : workBookPDF){
                    Map<String,Object> pdfData=(Map<String,Object>)pdfMap.get("workbook_pdf");
                    String fileName = (String) pdfData.get("filename");
                    String pdfPath = (String) pdfData.get("pdf_path");

                    Path filePath= Paths.get(pdfPath).normalize();
                    try(FileInputStream fis= new FileInputStream(filePath.toFile())){
                        ZipEntry zipEntry=new ZipEntry(fileName);
                        zipOutputStream.putNextEntry(zipEntry);

                        byte[] buffer=new byte[1024];
                        int length;
                        while((length=fis.read(buffer))>=0){
                            zipOutputStream.write(buffer,0,length);
                        }
                        zipOutputStream.closeEntry();
                    }
                }
            }
            ByteArrayResource resource=new ByteArrayResource(byteArrayOutputStream.toByteArray());


            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=\"files.zip\"")
                    .body(resource);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/answer/download/all")
    public ResponseEntity downloadFileanall() {
        Integer userId=(Integer)httpSession.getAttribute("userId");
        if(userId==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
        }
        try{
            List<WorkBook> workBook=workBookService.findWorkBookall(userId);
            List<Map<String,Object>> answerPDF=pdfService.getPdfsForanswer(workBook);
            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream(); //zip파일을 메모리에 저장하기 위한 바이트배열
            try(ZipOutputStream zipOutputStream=new ZipOutputStream(byteArrayOutputStream)){//zip파일을 생성하기 위한 스트림.위에 output데이터를 압축한다.
                for(Map<String, Object> pdfMap : answerPDF){//pdf정보 순회
                    Map<String,Object> pdfData=(Map<String,Object>)pdfMap.get("workbook_pdf");
                    String fileName = (String) pdfData.get("filename");
                    String pdfPath = (String) pdfData.get("pdf_path");
                    Path filePath= Paths.get(pdfPath).normalize();//경로를 path로 바꾸고,정규화한다.
                    try(FileInputStream fis= new FileInputStream(filePath.toFile())){//지정된 경로를 읽기위한 파일 스트림사용
                        ZipEntry zipEntry=new ZipEntry(fileName);//zip파일 내부에 개별파일들을 구별하기위해 사용
                        zipOutputStream.putNextEntry(zipEntry);//zip에 새 파일 등록
                        byte[] buffer=new byte[1024];
                        int length;
                        while((length=fis.read(buffer))>=0){//경로안에 있는 내용을 버퍼에 넣고
                            zipOutputStream.write(buffer,0,length);//스트림에 저장.
                        }
                        zipOutputStream.closeEntry();
                    }
                }
            }

            ByteArrayResource resource = new ByteArrayResource(byteArrayOutputStream.toByteArray());//메모리에 있는 스트림을 리소스로 변환

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=\"files.zip\"")
                    .body(resource);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

//    @DeleteMapping("/delete")
//    public ResponseEntity<String> bookdelete(){
//        workBookService.allbookdelete();
//        return ResponseEntity.ok("문제집 다 지우기 성공");
//    }

    @PostMapping("/front/processText")
    public ResponseEntity<Map<String, Object>> frontprocessText(@RequestBody String problemText,@RequestParam Integer userId) {
        log.debug("받은 문제 텍스트: " + problemText);

//        Integer userId = 7;
//        Integer userId = (Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }

        // 1단계: 문제 텍스트 요약
        Map<String, Object> summaryResult = chatGPTService.summarizeText(problemText);

        // 요약된 텍스트 추출
        String summarizedText = (String) summaryResult.get("content");

        if (summarizedText == null || summarizedText.trim().isEmpty()) {
            log.error("요약된 텍스트가 없습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "요약된 텍스트가 없습니다."));
        }

        log.debug("요약된 텍스트: " + summarizedText);

        // 2단계: 요약된 텍스트로 문제 생성
        Map<String, Object> questionResult = chatGPTService.generateQuestion(summarizedText);

        String Question = (String) questionResult.get("content");
        try {

            //questionResult.put("workBook", saveWorkBook.getWb_id());

            log.debug("생성된 질문: " + Question);

            // 3단계: 질문으로 답변 생성
            Map<String, Object> answerResult = chatGPTService.generateAnswer(Question);
            String answerText = (String) answerResult.get("content");
            WorkBook saveWorkBook = workBookService.findAndsaveWorkBook(Question, summarizedText,answerText, userId);

            if (answerText == null || answerText.trim().isEmpty()) {
                log.error("생성된 답변이 없습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "생성된 답변이 없습니다."));
            }
            pdfService.save(saveWorkBook.getWb_user_id(),userId);
            pdfService.answersave(saveWorkBook.getWb_user_id(),userId);
            log.debug("생성된 답변: " + answerText);
            QuestionAnswerResponse response=new QuestionAnswerResponse(saveWorkBook.getWb_user_id(),saveWorkBook.getWb_title(),Question,answerText);

            // 결과 반환
            return new ResponseEntity<>(Map.of("message",response), HttpStatus.OK);


        } catch (Exception e) {
            log.error("오류발생", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "WorkBook 저장 중 오류가 발생했습니다." + e.getMessage()));
        }

    }


    @PostMapping("/front/retext")
    public ResponseEntity<Object> frontretext(@RequestParam Integer userId){
//        Integer userId = 6;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook lastWorkBook=workBookService.findWorkBook(userId);
            Map<String,Object> questionResult = chatGPTService.regenerateQuestion(lastWorkBook.getWb_sumtext(), lastWorkBook.getWb_content()); //저장해둔 요약문자로 다시 생성.
            String newQuestion=(String) questionResult.get("content");
            log.debug("새 질문={}",newQuestion);

            //다음에 조회할때 필요할수 있으니 작성.

            Map<String, Object> answerResult = chatGPTService.generateAnswer(newQuestion);
            String answerText = (String) answerResult.get("content");
            WorkBook saveWorkBook = workBookService.findLastWorkBook(newQuestion,answerText, userId);

            if (answerText == null || answerText.trim().isEmpty()) {
                log.error("생성된 답변이 없습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "생성된 답변이 없습니다."));
            }

            log.debug("생성된 답변: " + answerText);
            QuestionAnswerResponse response=new QuestionAnswerResponse(saveWorkBook.getWb_user_id(),saveWorkBook.getWb_title(),newQuestion,answerText);

            // 결과 반환
            return new ResponseEntity<>(Map.of("message",response), HttpStatus.OK);
            // 결과 반환
            //return new ResponseEntity<>(Map.of("question", newQuestion, "answer", answerText, "workBook", saveWorkBook.getWb_user_id()), HttpStatus.OK);

        }catch(Exception e){
            log.error("오류발생");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "WorkBook 저장 중 오류가 발생했습니다."+e.getMessage()));
        }
    }

    @PatchMapping("/front/favorite")
    public ResponseEntity<Object> frontfavorite(@RequestParam Integer wb_id,@RequestParam Integer userId){
        //     Integer userId = 6;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook workBook=workBookService.findFavorite(wb_id,userId);
            return new ResponseEntity<>(Map.of("message","즐겨찾기가 완료되었습니다","wb_id", workBook.getWb_user_id(),"favorite",workBook.isWb_favorite()), HttpStatus.OK);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/front/answer/favorite")
    public ResponseEntity<Object> frontfavoriteanswer(@RequestParam Integer wb_id,@RequestParam Integer userId){
        //     Integer userId = 6;
        //Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook workBook=workBookService.findAnswerFavorite(wb_id,userId);
            return new ResponseEntity<>(Map.of("message","즐겨찾기가 완료되었습니다","wb_id", workBook.getWb_user_id(),"favorite",workBook.isWb_favorite_answer()), HttpStatus.OK);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/all")
    public ResponseEntity frontbookall(@RequestParam Integer userId){
//        Integer userId = 7;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            List<WorkBook> allbook=workBookService.findWorkBookall(userId);
            if(allbook.isEmpty()){
                return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
            }
            List<Map<String, Object>> pdfworkbook = pdfService.getPdfsForWorkbook(allbook);
            List<Map<String, Object>> completeData = allbook.stream()
                    .map(book -> {
                        Map<String, Object> pdfData = pdfworkbook.stream()
                                .filter(pdf -> pdf.get("wb_id").equals(book.getWb_id()))
                                .findFirst()
                                .orElse(Map.of("workbook_pdf", Map.of("filename", "", "pdf_path", "")));

                        return Map.of(
                                "wb_id", Optional.ofNullable(book.getWb_user_id()).orElse(-1),
                                "wb_title", Optional.ofNullable(book.getWb_title()).orElse(""),
                                "wb_create", Optional.ofNullable(book.getWb_create()).orElse(LocalDate.now()),
                                "wb_content", Optional.ofNullable(book.getWb_content()).orElse(""),
                                "favorite", book.isWb_favorite(),
                                "workbook_pdf", pdfData.get("workbook_pdf")
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("data", completeData));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    @GetMapping("/front/favorite/all")
    public ResponseEntity frontfavoriteall(@RequestParam Integer userId){
//        Integer userId = 5;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            List<WorkBook> allbook=workBookService.findfavoriteWorkBookall(userId);
            List<Map<String, Object>> answers = allbook.stream()
                    .map(book -> Map.of("wb_id",(Object) book.getWb_user_id(),"wb_title",book.getWb_title()
                            ,"wb_create",book.getWb_create(), "wb_content", book.getWb_content(),
                            "favorite",book.isWb_favorite()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("data",answers));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/search")
    public ResponseEntity frontselectbook(@RequestParam Integer wb_id,@RequestParam Integer userId){
//        Integer userId = 8;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook workBook=workBookService.findSearch(wb_id,userId);

            WorkBookPDF workBookPDF=pdfService.findByworkBook(workBook);
            Map<String,Object> workbookMap=Map.of(
                    "pdf_file",workBookPDF.getPdf_path() != null?workBookPDF.getPdf_path():"",
                    "filename",workBookPDF.getFileName() !=null?workBookPDF.getFileName():""
            );
            return ResponseEntity.ok(Map.of("wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title(),"wb_create",workBook.getWb_create(),
                    "content",workBook.getWb_content()
                    ,"workbook_pdf",workbookMap,"favorite",workBook.isWb_favorite()));
            //return ResponseEntity.ok(Map.of("wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title(),"wb_create",workBook.getWb_create(),"content",workBook.getWb_content(),"favorite",workBook.isWb_favorite()));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/answer/all")
    public ResponseEntity frontanswer(@RequestParam Integer userId){
//        Integer userId = 7;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            List<WorkBook> allbook=workBookService.findWorkBookall(userId);
            if(allbook.isEmpty()){
                return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
            }
            List<Map<String, Object>> pdfanswer = pdfService.getPdfsForanswer(allbook);
            List<Map<String, Object>> completeData = allbook.stream()
                    .map(book -> {
                        Map<String, Object> pdfData = pdfanswer.stream()
                                .filter(pdf -> pdf.get("wb_id").equals(book.getWb_id()))
                                .findFirst()
                                .orElse(Map.of("workbook_pdf", Map.of("filename", "", "pdf_path", "")));

                        return Map.of(
                                "wb_id", Optional.ofNullable(book.getWb_user_id()).orElse(-1),
                                "wb_title", Optional.ofNullable(book.getWb_title_answer()).orElse(""),
                                "wb_create", Optional.ofNullable(book.getWb_create()).orElse(LocalDate.now()),
                                "wb_answer", Optional.ofNullable(book.getWb_answer()).orElse(""),
                                "favorite", book.isWb_favorite_answer(),
                                "workbook_pdf", pdfData.get("workbook_pdf")
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("data", completeData));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/favorite/answer/all")
    public ResponseEntity frontanswerfavorite(@RequestParam Integer userId){
//        Integer userId = 6;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            List<WorkBook> allbook=workBookService.findfavoriteWorkBookall(userId);
            List<Map<String, Object>> answers = allbook.stream()
                    .map(book -> Map.of("wb_id",(Object) book.getWb_user_id(),"wb_title",book.getWb_title(), "wb_create",book.getWb_create(),"wb_answer", book.getWb_answer(),"favorite",book.isWb_favorite()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("data",answers));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/answer/search")
    public ResponseEntity frontselectanswer(@RequestParam Integer wb_id,@RequestParam Integer userId){
//        Integer userId = 8;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook workBook=workBookService.findSearch(wb_id,userId);
            AnswerPDF answerPDF=pdfService.anfindByworkBook(workBook);
            Map<String,Object> workbookMap=Map.of(
                    "pdf_file",answerPDF.getPdf_path() != null?answerPDF.getPdf_path():"",
                    "filename",answerPDF.getFileName() !=null?answerPDF.getFileName():""
            );
            return ResponseEntity.ok(Map.of("wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title_answer(),"wb_create",workBook.getWb_create(),
                    "wb_answer",workBook.getWb_answer(),"workbook_pdf",workbookMap,"favorite",workBook.isWb_favorite_answer()));
            //return ResponseEntity.ok(Map.of("wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title(),"wb_create",workBook.getWb_create(),"wb_answer",workBook.getWb_answer(),"favorite",workBook.isWb_favorite()));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/front/title")
    public ResponseEntity frontsettingtitle(@RequestParam Integer wb_id,@RequestParam String title,@RequestParam Integer userId){
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook workBook=workBookService.findSearchAndtitle(wb_id,userId,title);
            return ResponseEntity.ok(Map.of("message","제목변경 완료","wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title()));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/front/answer/title")
    public ResponseEntity frontsettinganswertitle(@RequestParam Integer wb_id,@RequestParam String title,@RequestParam Integer userId){
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook workBook=workBookService.findSearchAndanswertitle(wb_id,userId,title);
            return ResponseEntity.ok(Map.of("message","제목변경 완료","wb_id",workBook.getWb_user_id(),"wb_title",workBook.getWb_title_answer()));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/front/upload2")
    public ResponseEntity frontuploadWorkbook(@RequestParam Integer wb_id, @RequestParam("file")MultipartFile file,@RequestParam Integer userId){
//        Integer userId=6;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            pdfService.savedata(file,wb_id,userId);
            return ResponseEntity.ok(Map.of("message","저장완료"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    @PostMapping("/frontanswer/upload2")
    public ResponseEntity frontuploadanswer(@RequestParam Integer wb_id, @RequestParam("file")MultipartFile file,@RequestParam Integer userId){
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            pdfService.answersavedata(file,wb_id,userId);
            return ResponseEntity.ok(Map.of("message","저장완료"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/front/upload")
    public ResponseEntity frontuploadWorkbook2(@RequestParam Integer wb_id, @RequestParam("file")MultipartFile file,@RequestParam Integer userId){
//        Integer userId=6;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            pdfService.savedata2(file,wb_id,userId);
            return ResponseEntity.ok(Map.of("message","저장완료"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    @PostMapping("/front/answer/upload")
    public ResponseEntity frontuploadanswer2(@RequestParam Integer wb_id, @RequestParam("file")MultipartFile file,@RequestParam Integer userId){
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            pdfService.answersavedata2(file,wb_id,userId);
            return ResponseEntity.ok(Map.of("message","저장완료"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }





    @DeleteMapping("/front/delete")
    public ResponseEntity frontworkbookdelete(@RequestParam Integer wb_id,@RequestParam Integer userId){
//        Integer userId=5;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            workBookService.deleteSearch(wb_id,userId);
            return ResponseEntity.ok(Map.of("message","삭제완료"));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/download/{wb_id}")
    public ResponseEntity frontdownloadFile(@PathVariable Integer wb_id,@RequestParam Integer userId) {
//        Integer userId=6;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook workBook=workBookService.findSearch(wb_id,userId);
            WorkBookPDF workBookPDF=pdfService.findByworkBook(workBook);
            String pdfPath=workBookPDF.getPdf_path();
            Path filePath= Paths.get(pdfPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new FileNotFoundException("파일을 찾을수 없음: " + pdfPath);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=\""+resource.getFilename()+"\"")
                    .body(resource);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/answer/download/{wb_id}")
    public ResponseEntity frontdownloadFilean(@PathVariable Integer wb_id,@RequestParam Integer userId) {
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            WorkBook workBook=workBookService.findSearch(wb_id,userId);
            AnswerPDF answerPDF=pdfService.anfindByworkBook(workBook);
            String pdfPath=answerPDF.getPdf_path();
            Path filePath= Paths.get(pdfPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new FileNotFoundException("파일을 찾을수 없음: " + pdfPath);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=\""+resource.getFilename()+"\"")
                    .body(resource);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/download/all")
    public ResponseEntity frontdownloadFileall(@RequestParam Integer userId) {
//        Integer userId=6;
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            List<WorkBook> allbook=workBookService.findWorkBookall(userId);
            List<Map<String, Object>> workBookPDF=pdfService.getPdfsForWorkbook(allbook);

            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
            try(ZipOutputStream zipOutputStream=new ZipOutputStream(byteArrayOutputStream)){
                for(Map<String, Object> pdfMap : workBookPDF){
                    Map<String,Object> pdfData=(Map<String,Object>)pdfMap.get("workbook_pdf");
                    String fileName = (String) pdfData.get("filename");
                    String pdfPath = (String) pdfData.get("pdf_path");

                    Path filePath= Paths.get(pdfPath).normalize();
                    try(FileInputStream fis= new FileInputStream(filePath.toFile())){
                        ZipEntry zipEntry=new ZipEntry(fileName);
                        zipOutputStream.putNextEntry(zipEntry);

                        byte[] buffer=new byte[1024];
                        int length;
                        while((length=fis.read(buffer))>=0){
                            zipOutputStream.write(buffer,0,length);
                        }
                        zipOutputStream.closeEntry();
                    }
                }
            }
            ByteArrayResource resource=new ByteArrayResource(byteArrayOutputStream.toByteArray());


            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=\"files.zip\"")
                    .body(resource);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/front/answer/download/all")
    public ResponseEntity frontdownloadFileanall(@RequestParam Integer userId) {
//        Integer userId=(Integer)httpSession.getAttribute("userId");
//        if(userId==null){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error","세션이 만료되었으니 다시 사용해주세요"));
//        }
        try{
            List<WorkBook> workBook=workBookService.findWorkBookall(userId);
            List<Map<String,Object>> answerPDF=pdfService.getPdfsForanswer(workBook);
            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream(); //zip파일을 메모리에 저장하기 위한 바이트배열
            try(ZipOutputStream zipOutputStream=new ZipOutputStream(byteArrayOutputStream)){//zip파일을 생성하기 위한 스트림.위에 output데이터를 압축한다.
                for(Map<String, Object> pdfMap : answerPDF){//pdf정보 순회
                    Map<String,Object> pdfData=(Map<String,Object>)pdfMap.get("workbook_pdf");
                    String fileName = (String) pdfData.get("filename");
                    String pdfPath = (String) pdfData.get("pdf_path");
                    Path filePath= Paths.get(pdfPath).normalize();//경로를 path로 바꾸고,정규화한다.
                    try(FileInputStream fis= new FileInputStream(filePath.toFile())){//지정된 경로를 읽기위한 파일 스트림사용
                        ZipEntry zipEntry=new ZipEntry(fileName);//zip파일 내부에 개별파일들을 구별하기위해 사용
                        zipOutputStream.putNextEntry(zipEntry);//zip에 새 파일 등록
                        byte[] buffer=new byte[1024];
                        int length;
                        while((length=fis.read(buffer))>=0){//경로안에 있는 내용을 버퍼에 넣고
                            zipOutputStream.write(buffer,0,length);//스트림에 저장.
                        }
                        zipOutputStream.closeEntry();
                    }
                }
            }

            ByteArrayResource resource = new ByteArrayResource(byteArrayOutputStream.toByteArray());//메모리에 있는 스트림을 리소스로 변환

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=\"files.zip\"")
                    .body(resource);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}