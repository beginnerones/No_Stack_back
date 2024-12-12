package com.stone.microstone.workbook;

import com.stone.microstone.WorkBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerPdfRepository extends JpaRepository<AnswerPDF,Long> {
    AnswerPDF findByWorkBook(WorkBook workBook);
    AnswerPDF save(AnswerPDF answerPDF);

}
