package de.aksw.surface

import java.io.File
import org.apache.lucene.document.Document
import org.junit.runner.RunWith
import org.scalatest.Finders
import org.scalatest.FunSuite
import com.gilt.lucene.FSLuceneDirectory
import com.gilt.lucene.LuceneStandardAnalyzer
import com.gilt.lucene.ReadableLuceneIndex
import com.gilt.lucene.SimpleFSLuceneDirectoryCreator

import javax.annotation.Nonnull
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TestSurfaceFromIndexer extends FunSuite {

  test("indexable has to exist") {
    val nonExistingFile = new File("XXXXXXXXXXXXXXXXXXXXXXXXXX")
    intercept[IllegalArgumentException] {
      new SurfaceFormIndexer(nonExistingFile, new File("."))
    }
    intercept[IllegalArgumentException] {
      new SurfaceFormIndexer(new File("."), nonExistingFile)
    }

  }

  def deleteRecursively(f: File): Unit = {
    if (f.exists()) {
      if (f.isDirectory()) {
        f.listFiles().foreach {
          deleteRecursively(_)
        }
      }
      f.delete()
    }
  }

  test("indexes into given folder when needed")({
    val folderForIndexCreation = new File("target/test-classes/test-index")
    deleteRecursively(folderForIndexCreation)
    folderForIndexCreation.mkdirs()
    val indexable = new File("test/resources/test-surface-forms.ttl")
    assert(!SurfaceFormIndexer.indexExists(folderForIndexCreation), "index little file will be deleted, why it is there?")
    val testable = new SurfaceFormIndexer(indexable, folderForIndexCreation);
    assert(SurfaceFormIndexer.indexExists(folderForIndexCreation), "index should be created now")
    val index = new ReadableLuceneIndex
      with LuceneStandardAnalyzer
      with FSLuceneDirectory
      with SimpleFSLuceneDirectoryCreator
      with SpecifiableLuceneIndexPathProvider {
      override val path = folderForIndexCreation
    }
    val documents = index.allDocuments
    assert(7 == documents.size)
    val swat: String = "swat"
    def assertIsOnlyUriName(uriName :String, result : Iterable[Document]) = {
      assert(1 === result.size)
      val doc = result.toArray.apply(0)
      assert(uriName === doc.get("uriName"))
    }
    assertIsOnlyUriName(swat, testable.query(swat))
    assert(1 == testable.query("beat*").size)
    assert(0 == testable.query("groovy").size)
    assert(1 == testable.query("groovy*").size)
    assertIsOnlyUriName(swat, testable.query("Tactical*"))
    assertIsOnlyUriName(swat, testable.query("Operative*"))
    assert(2 == testable.query("java*").size)
  })

}