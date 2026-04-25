const fs = require('fs');
const path = require('path');

function processDir(dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            processDir(fullPath);
        } else if (fullPath.endsWith('.kt')) {
            let content = fs.readFileSync(fullPath, 'utf8');
            let hasInject = content.includes('import javax.inject.Inject');
            let hasSingleton = content.includes('import javax.inject.Singleton');
            
            if (hasInject || hasSingleton) {
                // remove them
                const orig = content;
                content = content.replace(/^import javax\.inject\.Inject[\r\n]+/gm, '');
                content = content.replace(/^import javax\.inject\.Singleton[\r\n]+/gm, '');
                
                if (content !== orig) {
                    let importsToAdd = '';
                    if (hasInject) importsToAdd += 'import javax.inject.Inject\n';
                    if (hasSingleton) importsToAdd += 'import javax.inject.Singleton\n';
                    
                    content = content.replace(/^(package[^\n]+[\r\n]+)/m, '$1\n' + importsToAdd);
                    fs.writeFileSync(fullPath, content);
                    console.log('Fixed ' + fullPath);
                }
            }
        }
    }
}

processDir('/data/data/com.termux/files/home/Sponsorflow/app/src/main/java/com');
