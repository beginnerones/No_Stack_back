package com.stone.microstone.workbook;

import com.stone.microstone.WorkBook;
import com.stone.microstone.locallogin.domain.entity.User;
import com.stone.microstone.locallogin.repository.UserRepository;
import com.stone.microstone.locallogin.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkBookService {
    private final WorkBookRepository workBookRepository;
    private final UserRepository userRepository;
    private final PdfService pdfService;

    public WorkBookService(WorkBookRepository workBookRepository, UserRepository userRepository,PdfService pdfService) {
        this.workBookRepository = workBookRepository;
        this.userRepository=userRepository;
        this.pdfService=pdfService;
    }
    @Transactional
    public WorkBook findAndsaveWorkBook(String content,String sumtext,String answer, int user_id) {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        int nextid=workBookRepository.findMaxUserid(user).orElse(0)+1;
        String title="문제집 "+nextid;
        String antitle="답안지 "+nextid;
        WorkBook newwork = new WorkBook();
        newwork.setUser(user);
        newwork.setWb_title(title);
        newwork.setWb_content(content);
        newwork.setWb_create(LocalDate.now());
        newwork.setWb_sumtext(sumtext);
        newwork.setWb_favorite(false);
        newwork.setWb_answer(answer);
        newwork.setWb_user_id(nextid);
        newwork.setWb_title_answer(antitle);
        newwork.setWb_favorite_answer(false);
        return workBookRepository.save(newwork);

    }
    @Transactional
    public WorkBook findLastWorkBook(String content, String answer,int user_id) {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));


        Optional<WorkBook> newwork = workBookRepository.findLastWorkBook(user);
        WorkBook newworkBook;
        if(newwork.isEmpty()) {
            throw new RuntimeException("기존 문제집이 존재하지 않음. User ID: " + user_id);
        }

        newworkBook=newwork.get();
        newworkBook.setUser(user);
        newworkBook.setWb_content(content);
        newworkBook.setWb_answer(answer);
        newworkBook.setWb_create(LocalDate.now());

        return workBookRepository.save(newworkBook);

    }
    @Transactional
    public WorkBook findWorkBook(int user_id) {
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        Optional<WorkBook> newwork = workBookRepository.findLastWorkBook(user);
        //WorkBook newworkBook=newwork.get();

        return newwork.orElseThrow(() -> new RuntimeException("기존 문제집이 존재하지 않음. User ID: " + user_id));

    }
    @Transactional
    public WorkBook findFavorite(int wb_id,int user_id){//문제찾고 즐겨찾기
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        Optional<WorkBook> newwork = workBookRepository.findByuserIdandUser(wb_id,user);
        WorkBook faworkBook=newwork.get();
        faworkBook.setWb_favorite(!faworkBook.isWb_favorite());
        return workBookRepository.save(faworkBook);

    }

    @Transactional
    public WorkBook findAnswerFavorite(int wb_id,int user_id){//문제찾고 즐겨찾기
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        Optional<WorkBook> newwork = workBookRepository.findByuserIdandUser(wb_id,user);
        WorkBook faworkBook=newwork.orElseThrow(()->new RuntimeException("문제를 찾을 수 없음. ID: "+wb_id));
        faworkBook.setWb_favorite_answer(!faworkBook.isWb_favorite_answer());
        return workBookRepository.save(faworkBook);

    }

    @Transactional
    public WorkBook findSearch(int wb_id,int user_id){  //문제찾기
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        Optional<WorkBook> newwork = workBookRepository.findByuserIdandUser(wb_id,user);
        WorkBook faworkBook=newwork.orElseThrow(()->new RuntimeException("문제를 찾을 수 없음. ID: "+wb_id));
        //faworkBook.setWb_favorite(true);
        return faworkBook;

    }

    @Transactional
    public void deleteSearch(int wb_id,int user_id) throws IOException {  //문제찾기
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));
        Optional<WorkBook> workBook = workBookRepository.findByuserIdandUser(wb_id,user);
        WorkBookPDF workBookPDF=pdfService.findByworkBook(workBook.get());
        if(workBook != null && workBookPDF.getPdf_path() !=null){
            Path path= Paths.get(workBookPDF.getPdf_path());
            Files.deleteIfExists(path);
        }
        AnswerPDF answerPDF=pdfService.anfindByworkBook(workBook.get());
        if(workBook != null &&answerPDF.getPdf_path() !=null){
            Path path= Paths.get(answerPDF.getPdf_path());
            Files.deleteIfExists(path);
        }
        workBookRepository.deleteById(wb_id,user);
        //faworkBook.setWb_favorite(true);
    }

    @Transactional
    public WorkBook findSearchAndtitle(int wb_id,int user_id,String title){  //문제찾기
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        Optional<WorkBook> newwork = workBookRepository.findByuserIdandUser(wb_id,user);
        WorkBook faworkBook=newwork.orElseThrow(()->new RuntimeException("문제를 찾을 수 없음. ID: "+wb_id));
        faworkBook.setWb_title(title);
        return faworkBook;

    }

    @Transactional
    public WorkBook findSearchAndanswertitle(int wb_id,int user_id,String title){  //문제찾기
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));

        Optional<WorkBook> newwork = workBookRepository.findByuserIdandUser(wb_id,user);
        WorkBook faworkBook=newwork.orElseThrow(()->new RuntimeException("문제를 찾을 수 없음. ID: "+wb_id));
        faworkBook.setWb_title_answer(title);
        return faworkBook;

    }

    @Transactional
    public List<WorkBook> findWorkBookall(int user_id){ //전체 문제찾기
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));
        List<WorkBook> newwork = workBookRepository.findByUser(user);
        return newwork;
    }

    @Transactional
    public List<WorkBook> findfavoriteWorkBookall(int user_id){//전체 즐겨찾기 문제찾기
        Optional<User> userOptional = userRepository.findById(user_id);
        User user = userOptional.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음. ID: " + user_id));
        List<WorkBook> newwork = workBookRepository.findByUserfavoirte(user);
        return newwork;
    }



    @Transactional
    public void allbookdelete(){
        workBookRepository.deleteAll();
    }



}
