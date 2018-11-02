import com.google.common.base.Charsets
import com.google.common.collect.Range
import com.google.common.collect.RangeSet
import com.google.common.collect.TreeRangeSet
import com.google.common.io.Files
import com.google.googlejavaformat.FormatterDiagnostic
import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.JavaCommentsHelper
import com.google.googlejavaformat.java.JavaFormatterOptions
import com.google.googlejavaformat.java.JavaInput
import com.google.googlejavaformat.java.JavaOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.*

class FormatTask extends DefaultTask {
    private static final Logger logger = LoggerFactory.getLogger(FormatTask)

    private List<FileCollection> filesToFormat = []
    private int maxWidthColumns = 100
    private int indentationMultiplier = 1
    private int maxParallelism = Runtime.getRuntime().availableProcessors()

    public void format(FileCollection files) {
        filesToFormat.add(files)
    }

    public void maxColumns(int columns) {
        this.maxWidthColumns = columns
    }

    public void indentationMultiplier(int multiplier) {
        this.indentationMultiplier = multiplier
    }

    @TaskAction
    public void doFormatting() {
        ExecutorService executor = Executors.newFixedThreadPool(maxParallelism)
        ExecutorCompletionService<Void> service = new ExecutorCompletionService(executor)
        int tasks = 0
       filesToFormat.each { fileCollection ->
            fileCollection.each { fileToFormat ->
                logger.info('Going to format {}', fileToFormat)
                service.submit({
                    formatFile(fileToFormat)
                })
                tasks++
            }
        }
        executor.shutdown()
        for (int i = 0; i < tasks; i++) {
            Future<Void> task = service.take()
            try {
                task.get()
            } catch (ExecutionException e) {
                throw new TaskExecutionException(this, e)
            }
        }
        logger.info('Finished formatting {} file(s)', tasks)
    }

    public void formatFile(File file) {
        JavaInput input = new JavaInput(Files.toString(file, Charsets.UTF_8))
        JavaFormatterOptions opts = JavaFormatterOptions.defaultOptions()
        JavaCommentsHelper commentsHelper = new JavaCommentsHelper('\n', opts)
        JavaOutput output = new JavaOutput('\n', input, commentsHelper);
        List<FormatterDiagnostic> errors = []
        Formatter.format(input, output, opts)
        if (!errors.isEmpty()) {
            logger.warn("Could not format {}, {} error(s)", file, errors.size())
            for (FormatterDiagnostic diagnostic : errors) {
                logger.warn(diagnostic.toString())
            }
        }
    }
}
