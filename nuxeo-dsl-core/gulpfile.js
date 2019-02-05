const gulp = require("gulp"),
      $ = require('gulp-load-plugins')();
      mocha = require('gulp-mocha'),
      util = require('gulp-util'),
      print = require('gulp-print'),
      babel = require('gulp-babel'),
      runSequence = require('run-sequence'),
      concat = require('gulp-concat')
      dest = gulp.dest;





gulp.task('lib', function () {
    return gulp.src(['lib/*.js'], { cwd: 'src/main/js'})
        .pipe(dest('target/generated-resources/js/lib'));
});

gulp.task('build', gulp.series('lib',function () {
   return gulp.src(['nuxeo_dsl*.js'], { cwd: 'src/main/js' })
        .pipe(babel({
            presets: ['env'],
          compact: false,
          ignore: '*_spec.js'}))
        .pipe(dest('target/generated-resources/js'))
        .pipe(print())
}));

gulp.task('test', gulp.series('build', function () {
    return gulp.src(['*_spec.js'], { cwd: 'src/main/js',read: false })
        .pipe(mocha({ reporter: 'spec' }))
        .on('error', util.log);
}));


gulp.task('watch', function () {
    gulp.watch(['src/main/js/nuxeo_dsl*.js'], ['test']);
});

gulp.task('default', () => {
    gulp.start('test');
    gulp.start('watch');
});