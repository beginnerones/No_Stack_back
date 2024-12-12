package com.stone.microstone.workbook;

import com.stone.microstone.WorkBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkbookPdfRepository extends JpaRepository<WorkBookPDF,Long> {
    WorkBookPDF findByWorkBook(WorkBook workBook);
    WorkBookPDF save(WorkBookPDF workBookPDF);
}
