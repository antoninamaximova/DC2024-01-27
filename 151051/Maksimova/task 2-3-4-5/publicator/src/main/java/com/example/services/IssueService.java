package com.example.services;

import com.example.dto.IssueRequestTo;
import com.example.dto.IssueResponseTo;
import com.example.entities.Issue;
import com.example.exceptions.DeleteException;
import com.example.exceptions.DuplicationException;
import com.example.exceptions.NotFoundException;
import com.example.exceptions.UpdateException;
import com.example.mapper.IssueListMapper;
import com.example.mapper.IssueMapper;
import com.example.repository.EditorRepository;
import com.example.repository.IssueRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
@CacheConfig(cacheNames = "issueCache")
public class IssueService {
    @Autowired
    IssueMapper issueMapper;
    @Autowired
    IssueRepository issueDao;
    @Autowired
    IssueListMapper issueListMapper;
    @Autowired
    EditorRepository editorRepository;
    @Cacheable(value = "issues", key = "#id", unless = "#result == null")
    public IssueResponseTo getIssueById(@Min(0) Long id) throws NotFoundException {
        Optional<Issue> issue = issueDao.findById(id);
        return issue.map(value -> issueMapper.issueToIssueResponse(value)).orElseThrow(() -> new NotFoundException("Issue not found!", 40004L));
    }

    @Cacheable(cacheNames = "issues")
    public List<IssueResponseTo> getIssues(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageable;
        if (sortOrder != null && sortOrder.equals("asc")) {
            pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy).ascending());
        } else {
            pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy).descending());
        }
        Page<Issue> issues = issueDao.findAll(pageable);
        return issueListMapper.toIssueResponseList(issues.toList());
    }
    @CacheEvict(cacheNames = "issues", allEntries = true)
    public IssueResponseTo saveIssue(@Valid IssueRequestTo issue) throws DuplicationException {
        Issue issueToSave = issueMapper.issueRequestToIssue(issue);
        if (issueDao.existsByTitle(issueToSave.getTitle())) {
            throw new DuplicationException("Title duplication", 40005L);
        }
        if (issue.getEditorId() != null) {
            issueToSave.setEditor(editorRepository.findById(issue.getEditorId()).orElseThrow(() -> new NotFoundException("Editor not found!", 40004L)));
        }
        return issueMapper.issueToIssueResponse(issueDao.save(issueToSave));
    }
    @Caching(evict = { @CacheEvict(cacheNames = "issues", key = "#id"),
            @CacheEvict(cacheNames = "issues", allEntries = true) })
    public void deleteIssue(@Min(0) Long id) throws DeleteException {
        if (!issueDao.existsById(id)) {
            throw new DeleteException("Issue not found!", 40004L);
        } else {
            issueDao.deleteById(id);
        }
    }
    @CacheEvict(cacheNames = "issues", allEntries = true)
    public IssueResponseTo updateIssue(@Valid IssueRequestTo issue) throws UpdateException {
        Issue issueToUpdate = issueMapper.issueRequestToIssue(issue);
        if (!issueDao.existsById(issue.getId())) {
            throw new UpdateException("Post not found!", 40004L);
        } else {
            if (issue.getEditorId() != null) {
                issueToUpdate.setEditor(editorRepository.findById(issue.getEditorId()).orElseThrow(() -> new NotFoundException("Editor not found!", 40004L)));
            }
            return issueMapper.issueToIssueResponse(issueDao.save(issueToUpdate));
        }
    }

    public List<IssueResponseTo> getIssueByCriteria(List<String> stickerName, List<Long> stickerId, String editorLogin, String title, String content) {
        return issueListMapper.toIssueResponseList(issueDao.findIssuesByParams(stickerName, stickerId, editorLogin, title, content));
    }
}
