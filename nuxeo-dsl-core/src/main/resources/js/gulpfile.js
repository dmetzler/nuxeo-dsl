const gulp = require("gulp"),
      $ = require('gulp-load-plugins')();
      mocha = require('gulp-mocha'),
      util = require('gulp-util'),
      print = require('gulp-print'),
      babel = require('gulp-babel'),
      runSequence = require('run-sequence'),
      concat = require('gulp-concat');



gulp.task('test', ['build'], function () {
    return gulp.src(['*_spec.js'], { read: false })
        .pipe(mocha({ reporter: 'spec' }))
        .on('error', util.log);
});


gulp.task('build', function () {
   return gulp.src(['nuxeo_dsl*.js'])
        .pipe(babel({
            presets: ['env'],
          compact: false,
          ignore: '*_spec.js'}))

        .pipe(print())
        .pipe(gulp.dest('dist'))
        .on('error', util.log);
});


gulp.task('watch', function () {
    gulp.watch(['nuxeo_dsl*.js'], ['test']);
});

gulp.task('default', () => {
    gulp.start('test');
    gulp.start('watch');
});