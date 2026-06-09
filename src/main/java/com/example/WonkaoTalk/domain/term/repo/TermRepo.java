package com.example.WonkaoTalk.domain.term.repo;

import com.example.WonkaoTalk.domain.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepo extends JpaRepository<Term, Long> {

}
