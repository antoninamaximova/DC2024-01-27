package com.padabied.dc_rest.repository.interfaces;

import com.padabied.dc_rest.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface StoryRepository extends JpaRepository <Story, Long> {
}
