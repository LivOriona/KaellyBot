package commands.model;

import data.Constants;
import data.Guild;
import enums.Language;
import exceptions.BadUseCommandDiscordException;
import exceptions.BasicDiscordException;
import exceptions.DiscordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import util.Reporter;
import util.Translator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by steve on 14/07/2016.
 */
public abstract class AbstractCommand implements Command {

    private final static Logger LOG = LoggerFactory.getLogger(AbstractCommand.class);

    protected DiscordException commandForbidden;
    protected DiscordException notUsableInMp;

    protected String name;
    protected String pattern;
    protected boolean isPublic;
    protected boolean isUsableInMP;
    protected boolean isAdmin;

    protected AbstractCommand(String name, String pattern){
        super();
        this.name = name;
        this.pattern = pattern;
        this.isPublic = true;
        this.isUsableInMP = true;
        this.isAdmin = false;
        commandForbidden = new BasicDiscordException("exception.basic.command_forbidden");
        notUsableInMp = new BasicDiscordException("exception.basic.not_usable_in_mp");
    }

    @Override
    public boolean request(IMessage message) {
        boolean isFound = false;
        try {
            Language lg = Translator.getLanguageFrom(message.getChannel());
            Matcher m = getMatcher(message);
            isFound = m.find();

            // Caché si la fonction est désactivée/réservée aux admin et que l'auteur n'est pas super-admin
            if ((! isPublic() || isAdmin()) && message.getAuthor().getLongID() != Constants.authorId && message.getAuthor().getLongID() != Constants.authorId2)
                return false;
            // La commande est trouvée
            if(isFound) {
                // Mais n'est pas utilisable en MP
                if (! isUsableInMP() && message.getChannel().isPrivate()) {
                    notUsableInMp.throwException(message, this, lg);
                    return false;
                }
                // Mais est désactivée par la guilde
                else if (! message.getChannel().isPrivate() && message.getAuthor().getLongID() != Constants.authorId && message.getAuthor().getLongID() != Constants.authorId2
                        && isForbidden(Guild.getGuild(message.getGuild()))) {
                    commandForbidden.throwException(message, this, lg);
                    return false;
                }
            }
            // Mais est mal utilisée
            else if (message.getContent().startsWith(getPrefix(message) + getName()))
                new BadUseCommandDiscordException().throwException(message, this, lg);

        } catch (Exception e) {
            Reporter.report(e, message.getGuild(), message.getChannel(), message.getAuthor(), message);
            LOG.error("request", e);
        }
        return isFound;
    }

    @Override
    public boolean isForbidden(Guild g){
        return g.getForbiddenCommands().containsKey(getName());
    }

    @Override
    public Matcher getMatcher(IMessage message){
        String prefixe = getPrefix(message);
        return Pattern.compile("^" + Pattern.quote(prefixe) + name + pattern + "$").matcher(message.getContent());
    }

    @Override
    public String getPrefix(IMessage message){
        String prefix = "";
        if (! message.getChannel().isPrivate())
            prefix = Guild.getGuild(message.getGuild()).getPrefix();
        return prefix;
    }

    protected String getPrefixMdEscaped(IMessage message){
        String prefix = "";
        if (! message.getChannel().isPrivate())
            prefix = Guild.getGuild(message.getGuild()).getPrefix();
        prefix = prefix.replaceAll("\\*", "\\\\*") // Italic & Bold
                .replaceAll("_", "\\_")          // Underline
                .replaceAll("~", "\\~")          //Strike
                .replaceAll("\\`", "\\\\`");         //Code
        return prefix;
    }

    protected boolean isUserHasEnoughRights(IMessage message){
        return ! message.getChannel().isPrivate() &&
                (message.getAuthor().getLongID() == Constants.authorId
                || message.getAuthor().getLongID() == Constants.authorId2
                || message.getAuthor().getPermissionsForGuild(message.getGuild()).contains(Permissions.MANAGE_SERVER)
                || message.getChannel().getModifiedPermissions(message.getAuthor()).contains(Permissions.MANAGE_SERVER));

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public void setPublic(boolean isPublic){
        this.isPublic = isPublic;
    }

    @Override
    public boolean isPublic(){ return isPublic; }

    @Override
    public boolean isUsableInMP(){ return isUsableInMP; }

    @Override
    public void setUsableInMP(boolean isUsableInMP) {
        this.isUsableInMP = isUsableInMP;
    }

    @Override
    public boolean isAdmin() {
        return isAdmin;
    }

    @Override
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}
