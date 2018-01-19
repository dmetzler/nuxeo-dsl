const gulp = require("gulp"),
    lazypipe = require('lazypipe'),
    sourcemaps = require('gulp-sourcemaps'),
    rev = require('gulp-rev'),
    cssnano = require('gulp-cssnano'),
    uglify = require('gulp-uglify'),
    useref = require("gulp-useref"),
    revReplace = require("gulp-rev-replace")
    rename = require("gulp-rename"),
    gulpIf = require('gulp-if'),
    inject = require('gulp-inject'),
    babel = require('gulp-babel'),
    del = require('del'),
    print = require('gulp-print'),
    browserSync = require('browser-sync').create(),
    reload      = browserSync.reload;

const bowerLibFiles = require('main-bower-files');

const lib = require('./lib');

const images = ['img/**/*.png']
const extras = ['favicon.ico','sample.ndl','partials/**/*','img/**/*']


const config = {
    dist: 'target/classes/web/nuxeo.war/ui/'
}

const babelTask = lazypipe()
    .pipe(babel, {presets: ['es2015']});
const initTask = lazypipe()
    .pipe(sourcemaps.init);
const jsTask = lazypipe()
    .pipe(uglify);
const cssTask = lazypipe()
    .pipe(cssnano);

var DIST = 'target/classes/web/nuxeo.war/dslstudio';


gulp.task('clean', () => del([config.dist], { dot: true }));

gulp.task('extras', ['clean'], function() {
    gulp.src(extras, { base: '.'}) 
        .pipe(print())
        .pipe(gulp.dest( DIST ));
});


gulp.task('build', ['clean', 'inject','extras'], () => {
    const manifest = gulp.src('.tmp/rev-manifest.json');

    return gulp.src('index-dev.html')
        .pipe(rename("index.html"))
        //init sourcemaps
        .pipe(useref(
            {},
            lazypipe().pipe(() => gulpIf('bower_components/nuxeodsl_parser/index.js', babelTask())),
            initTask
        ))
        //Todo find a way to exclude nuxeo_dsl or make nuxeo_dsl uglifiable
        //.pipe(gulpIf('*.js', jsTask()))
        .pipe(gulpIf('*.png', print()))
        .pipe(gulpIf('*.css', cssTask()))
        .pipe(gulpIf('**/*.!(html)', rev()))
        .pipe(revReplace({manifest: manifest}))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(DIST));
});

gulp.task('inject', () => {
    return gulp.src('index-dev.html')
        .pipe(inject(gulp.src(bowerLibFiles(), {read: false}), {
            name: 'bower',
            relative: true
        }))
        .pipe(inject(gulp.src(lib.JS), {relative: true}))
        .pipe(inject(gulp.src(lib.CSS), {relative: true}))
        .pipe(gulp.dest(''));
});

gulp.task('serve', ['inject'], () => {

    // Serve files from the root of this project
    browserSync.init({
        server: {
            baseDir: "./",
            index: "index-dev.html"
        }
    });

    gulp.watch("index-dev.html").on("change", reload);
    gulp.watch("partials/*.html").on("change", reload);
    gulp.watch("js/*.js").on("change", reload);
    gulp.watch("codemirror/*").on("change", reload);
    gulp.watch("nomnoml/*").on("change", reload);
    gulp.watch("css/*.css").on("change", reload);
});


gulp.task('serve-dist', ['build'], () => {

    // Serve files from the root of this project
    browserSync.init({
        server: {
            baseDir: "./" + DIST,
            index: "index.html"
        }
    });
});

gulp.task('default', () => {
    gulp.start('serve');
});
