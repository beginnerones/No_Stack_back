package com.stone.microstone.workbook;

import com.stone.microstone.WorkBook;
import com.stone.microstone.locallogin.domain.entity.User;
import com.stone.microstone.locallogin.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PdfService {

    private WorkbookPdfRepository workbookPdfRepository;
    private AnswerPdfRepository answerPdfRepository;
    private WorkBookRepository workbookRepository;
    private UserRepository userRepository;
    private static final String UPLOAD_DIR="uploads";

    @Autowired
    public PdfService(WorkbookPdfRepository workbookPdfRepository, AnswerPdfRepository answerPdfRepository, WorkBookRepository workbookRepository,UserRepository userRepository) {
        this.workbookPdfRepository = workbookPdfRepository;
        this.answerPdfRepository = answerPdfRepository;
        this.workbookRepository = workbookRepository;
        this.userRepository = userRepository;
    }

    public WorkBookPDF save( int wb_id,int user_id) throws IOException {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        WorkBook workBook = workbookRepository.findByuserIdandUser(wb_id,user).orElseThrow(() -> new RuntimeException("문제집이 존재하지 않음"));

        WorkBookPDF pdf=new WorkBookPDF();
        pdf.setWorkBook(workBook);
        return workbookPdfRepository.save(pdf);
    }

    public WorkBookPDF savedata(MultipartFile file, int wb_id,int user_id) throws IOException {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        WorkBook workBook = workbookRepository.findByuserIdandUser(wb_id,user).orElseThrow(() -> new RuntimeException("문제집이 존재하지 않음"));
        WorkBookPDF pdf=workbookPdfRepository.findByWorkBook(workBook);
        pdf.setFileName(file.getOriginalFilename());
        pdf.setPdf_data(file.getBytes());
        return workbookPdfRepository.save(pdf);
    }

    public WorkBookPDF savedata2(MultipartFile file, int wb_id,int user_id) throws IOException {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        WorkBook workBook = workbookRepository.findByuserIdandUser(wb_id,user).orElseThrow(() -> new RuntimeException("문제집이 존재하지 않음"));
        WorkBookPDF pdf=workbookPdfRepository.findByWorkBook(workBook);
        String filePath=savePath(file, file.getOriginalFilename());
        pdf.setFileName(file.getOriginalFilename());
        pdf.setPdf_path(filePath);
        return workbookPdfRepository.save(pdf);
    }

    public WorkBookPDF findByworkBook(WorkBook wb) {


        return workbookPdfRepository.findByWorkBook(wb);
    }

    public AnswerPDF answersave(int wb_id,int user_id) throws IOException {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        WorkBook workBook = workbookRepository.findByuserIdandUser(wb_id,user).orElseThrow(() -> new RuntimeException("문제집이 존재하지 않음"));

        AnswerPDF answerPDF=new AnswerPDF();

        answerPDF.setWorkBook(workBook);
        return answerPdfRepository.save(answerPDF);
    }

    public AnswerPDF answersavedata(MultipartFile file, int wb_id,int user_id) throws IOException {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        WorkBook workBook = workbookRepository.findByuserIdandUser(wb_id,user).orElseThrow(() -> new RuntimeException("문제집이 존재하지 않음"));

        AnswerPDF answerPDF=answerPdfRepository.findByWorkBook(workBook);
        answerPDF.setFileName(file.getOriginalFilename());
        answerPDF.setPdf_data(file.getBytes());
        return answerPdfRepository.save(answerPDF);
    }

    public AnswerPDF answersavedata2(MultipartFile file, int wb_id,int user_id) throws IOException {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        WorkBook workBook = workbookRepository.findByuserIdandUser(wb_id,user).orElseThrow(() -> new RuntimeException("문제집이 존재하지 않음"));

        AnswerPDF answerPDF=answerPdfRepository.findByWorkBook(workBook);
        String filePath=savePath(file, file.getOriginalFilename());
        answerPDF.setFileName(file.getOriginalFilename());
        answerPDF.setPdf_path(filePath);
        return answerPdfRepository.save(answerPDF);
    }

    public AnswerPDF anfindByworkBook(WorkBook wb) {
        return answerPdfRepository.findByWorkBook(wb);
    }

    public List<Map<String,Object>> getPdfsForWorkbook(List<WorkBook>workBooks){
        return workBooks.stream()
                .map(workBook -> {
                    WorkBookPDF workbookPdfs = workbookPdfRepository.findByWorkBook(workBook);
                    return Map.of(
                            "wb_id", workBook.getWb_id(),
                            "workbook_pdf", Map.of(
                                    "filename", Optional.ofNullable(workbookPdfs).map(WorkBookPDF::getFileName).orElse(""),
                                    "pdf_path", Optional.ofNullable(workbookPdfs).map(WorkBookPDF::getPdf_path).orElse("")
                                    //"pdf_data", workbookPdfs != null ? workbookPdfs.getPdf_data() : null
                            )
                    );
                })
                .collect(Collectors.toList());
    }

    public List<Map<String,Object>> getPdfsForanswer(List<WorkBook>workBooks){
        return workBooks.stream()
                .map(workBook -> {
                    AnswerPDF answerPDF = answerPdfRepository.findByWorkBook(workBook);
                    return Map.of(
                            "wb_id", workBook.getWb_id(),
                            "workbook_pdf", Map.of(
                                    "filename", Optional.ofNullable(answerPDF).map(AnswerPDF::getFileName).orElse(""),
                                    "pdf_path", Optional.ofNullable(answerPDF).map(AnswerPDF::getPdf_path).orElse("")
                                    //"pdf_data", answerPDF != null ? answerPDF.getPdf_data() : null
                            )
                    );
                })
                .collect(Collectors.toList());
    }

    private String savePath(MultipartFile file,String fileName) throws IOException{
        Path uploadpath= Paths.get(UPLOAD_DIR);
        if(!Files.exists(uploadpath)){
            Files.createDirectories(uploadpath);
        }
        Path filePath=uploadpath.resolve(fileName);
        Files.write(filePath,file.getBytes());
        log.debug(filePath.toString());
        return filePath.toString();
    }

}
