package cn.file.overall;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by 余庚鑫 on 2017/10/13.
 * 配置文件
 */
@Component
public class Properties {
    /**
     * token
     */
    public static String JWT_SECRET;

    /**
     * 文件保存地址
     *
     * @param jwtSecret
     */
    public static String AFFIX_DIR;

    @Value("#{config['jwt.secret']}")
    public void setJwtSecret(String jwtSecret) {
        JWT_SECRET = jwtSecret;
    }

    @Value("#{config['affix.dir']}")
    public void setAffixDir(String affixDir) {
        AFFIX_DIR = affixDir;
    }
}
