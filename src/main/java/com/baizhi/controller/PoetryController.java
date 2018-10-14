package com.baizhi.controller;

import com.baizhi.entity.Result;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RequestMapping("/poetry")
@Controller("poetryController")
public class PoetryController {

    @RequestMapping("/search")
    public ModelAndView searchIndex(ModelAndView modelAndView,String keyWord,Integer nowPage,Integer pageSize){
        try {
            FSDirectory fsDirectory = FSDirectory.open(Paths.get("F:\\lucene\\index4"));
            IndexReader indexReader = DirectoryReader.open(fsDirectory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);

            Query query = new QueryParser("content", new StandardAnalyzer()).parse(keyWord);
            //====高亮
            Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<span style='color:red'>", "</span>"), new QueryScorer(query));
            //======分页
            TopDocs topDocs = null;
            if(nowPage==1){
                topDocs = indexSearcher.search(query,pageSize);
            }else{
                topDocs = indexSearcher.search(query,(nowPage-1)*pageSize);
                ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                ScoreDoc lastScoreDoc = scoreDocs[scoreDocs.length - 1];
                topDocs = indexSearcher.searchAfter(lastScoreDoc,query,pageSize);
            }
            //=======

            System.out.println("命中结果的数量："+topDocs.totalHits);
            modelAndView.addObject("count",topDocs.totalHits);

            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            List<Result> list = new ArrayList<>();
            Result result = null;
            for (ScoreDoc scoreDoc : scoreDocs) {
                result = new Result();
                int docId = scoreDoc.doc;
                Document document = indexReader.document(docId);

                //获取最佳片段（高亮）
                String bestFragment = highlighter.getBestFragment(new StandardAnalyzer(), "content", document.get("content"));

                result.setId(Integer.parseInt(document.get("id")));
                result.setName(document.get("name"));
                result.setTitle(document.get("title"));
                result.setContent(document.get("content"));

                list.add(result);
            }
            modelAndView.addObject("list",list);
            modelAndView.setViewName("result");
            indexReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (InvalidTokenOffsetsException e) {
            e.printStackTrace();
        }
        return modelAndView;
    }
}
