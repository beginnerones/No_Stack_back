package com.stone.microstone.workbook;

import com.stone.microstone.WorkBook;
import com.stone.microstone.locallogin.domain.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class WorkBookRepositoryImpl implements WorkBookRepository {

    @PersistenceContext
    private EntityManager em;

    public WorkBookRepositoryImpl(EntityManager em) {
        this.em=em;
    }



    @Override
    public WorkBook save(WorkBook workBook) {
        if(workBook.getWb_id()==0){
            em.persist(workBook);
        }else{
            em.merge(workBook);
        }
        return workBook;
    }

    @Override
    public Optional<WorkBook> findById(int id) {
        List<WorkBook> result=em.createQuery("SELECT w FROM WorkBook w WHERE w.wb_id=:id")
                .setParameter("id",id)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public Optional<WorkBook> findByIdandUser(int id, User user) {
        List<WorkBook> result=em.createQuery("SELECT w FROM WorkBook w WHERE w.wb_id=:id AND w.user=:user",WorkBook.class)
                .setParameter("id",id)
                .setParameter("user",user)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public Optional<WorkBook> findByuserIdandUser(int id, User user) {
        List<WorkBook> result=em.createQuery("SELECT w FROM WorkBook w WHERE w.wb_user_id=:id AND w.user=:user",WorkBook.class)
                .setParameter("id",id)
                .setParameter("user",user)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public void deleteById(int id,User user) {
        em.createQuery("DELETE FROM WorkBook wb WHERE wb.wb_user_id=:id AND wb.user=:user")
                .setParameter("id",id)
                .setParameter("user",user)
                .executeUpdate();
    }

    @Override
    public List<WorkBook> findAll() {
        return em.createQuery("SELECT w FROM WorkBook w",WorkBook.class).getResultList();
    }

    @Override
    public Optional<WorkBook> findLastWorkBook(User user) {
        return em.createQuery("SELECT w FROM WorkBook w WHERE w.user=:user ORDER BY w.wb_user_id DESC",WorkBook.class )
                .setParameter("user",user)
                .setMaxResults(1)
                .getResultList().stream().findFirst();
    }

    @Override
    public List<WorkBook> findByUserEmailAndLoginInfo(String email, String loginInfo) {
        return em.createQuery("SELECT w FROM WorkBook w WHERE w.user.email = :email AND w.user.loginInfo = :loginInfo", WorkBook.class)
                .setParameter("email", email)
                .setParameter("loginInfo", loginInfo)
                .getResultList();
    }

    @Override
    public Optional<WorkBook> findLastWorkBookByUserEmailAndLoginInfo(String email, String loginInfo) {
        List<WorkBook> result = em.createQuery("SELECT w FROM WorkBook w WHERE w.user.email = :email AND w.user.loginInfo = :loginInfo ORDER BY w.wb_create DESC", WorkBook.class)
                .setParameter("email", email)
                .setParameter("loginInfo", loginInfo)
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public List<WorkBook> findByUser(User user) {
        return em.createQuery("SELECT w FROM WorkBook w WHERE w.user = :user", WorkBook.class)
                .setParameter("user", user)
                .getResultList();
    }

    @Override
    public List<WorkBook> findByUserfavoirte(User user) {
        return em.createQuery("SELECT w FROM WorkBook w WHERE w.user = :user AND w.wb_favorite=true", WorkBook.class)
                .setParameter("user", user)
                .getResultList();
    }

    @Override
    public Optional<WorkBook> findTopByUserOrderByWbCreateDesc(User user) {
        List<WorkBook> result = em.createQuery("SELECT w FROM WorkBook w WHERE w.user = :user ORDER BY w.wb_create DESC", WorkBook.class)
                .setParameter("user", user)
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst();
    }

    public void deleteAll() {
        em.createQuery("DELETE FROM WorkBook").executeUpdate();
        em.createNativeQuery("ALTER TABLE WorkBook AUTO_INCREMENT = 1").executeUpdate();
    }

    @Override
    public Optional<Integer> findMaxUserid(User user) {
        Integer maxUserId = em.createQuery(
                "SELECT MAX(w.wb_user_id) FROM WorkBook w WHERE w.user = :user", Integer.class)
                .setParameter("user", user)
                .getSingleResult();
        return Optional.ofNullable(maxUserId);
    }


}
