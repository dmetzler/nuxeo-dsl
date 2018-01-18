const gulp = require("gulp"),
      mocha = require('gulp-mocha'),
      util = require('gulp-util');
 
gulp.task('test', function () {
    return gulp.src(['*_spec.js'], { read: false })
        .pipe(mocha({ reporter: 'spec' }))
        .on('error', util.log);
});
 
gulp.task('watch-test', function () {
    gulp.watch(['nuxeo_dsl.js','nuxeo_dsl_spec.js'], ['test']);
});

gulp.task('default', () => {
    gulp.start('test');
    gulp.start('watch-test');
});