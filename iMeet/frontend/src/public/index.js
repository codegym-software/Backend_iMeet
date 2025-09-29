
        const tryRequire = (path) => {
        	try {
        	const image = require(`${path}`);
        	return image
        	} catch (err) {
        	return false
        	}
        };

        export default {
        
	questionMark: require('./questionMark.png'),
	Signup_chrislee70l1tDAI6rMunsplash1: tryRequire('./Signup_chrislee70l1tDAI6rMunsplash1.png') || require('./questionMark.png'),
	Login_Vector: tryRequire('./Login_Vector.png') || require('./questionMark.png'),
	Login_Vector_1: tryRequire('./Login_Vector_1.png') || require('./questionMark.png'),
	Login_Vector_2: tryRequire('./Login_Vector_2.png') || require('./questionMark.png'),
	Login_Vector_3: tryRequire('./Login_Vector_3.png') || require('./questionMark.png'),
	Login_Vector_4: tryRequire('./Login_Vector_4.png') || require('./questionMark.png'),
	Login_Line2: tryRequire('./Login_Line2.png') || require('./questionMark.png'),
}