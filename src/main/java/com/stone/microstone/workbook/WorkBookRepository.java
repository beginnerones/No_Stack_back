package com.stone.microstone.workbook;

import com.stone.microstone.WorkBook;
import com.stone.microstone.locallogin.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface WorkBookRepository {

    WorkBook save(WorkBook workBook);
    Optional<WorkBook> findById(int id);
    Optional<WorkBook> findByIdandUser(int id, User user);
    Optional<WorkBook> findByuserIdandUser(int id, User user);
    void deleteById(int id,User user);
    List<WorkBook> findAll();

    Optional<WorkBook> findLastWorkBook(User user);

    List<WorkBook> findByUserEmailAndLoginInfo(String email, String loginInfo);

    Optional<WorkBook> findLastWorkBookByUserEmailAndLoginInfo(String email, String loginInfo);

    List<WorkBook> findByUser(User user);

    List<WorkBook> findByUserfavoirte(User user);

    Optional<WorkBook> findTopByUserOrderByWbCreateDesc(User user);

    void deleteAll();

    Optional<Integer> findMaxUserid(User user);
}
