/*
 * Copyright (c) 2009-2011. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.security.Principal;
import java.util.Set;

import com.jts.fortress.ReviewMgr;
import com.jts.fortress.ReviewMgrFactory;
import com.jts.fortress.AccessMgr;
import com.jts.fortress.AccessMgrFactory;
import com.jts.fortress.SecurityException;
import com.jts.fortress.constants.GlobalErrIds;
import com.jts.fortress.rbac.RoleUtil;
import com.jts.fortress.rbac.User;
import com.jts.fortress.rbac.Role;
import com.jts.fortress.rbac.Session;
import com.jts.fortress.rbac.UserRole;
import com.jts.fortress.realm.tomcat.TcPrincipal;
import com.jts.fortress.util.attr.VUtil;
import com.jts.fortress.util.time.CUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This class is for components that use Websphere and Tomcat Container SPI's to provide
 * Java EE Security capabilities.  These APIs may be called by external programs as needed though the recommended
 * practice is to use Fortress Core APIs like {@link com.jts.fortress.AccessMgr} and {@link com.jts.fortress.ReviewMgr}.
 *
 * @author smckinn
 * @created January 13, 2010
 */
public class J2eePolicyMgrImpl implements J2eePolicyMgr
{
    private static final String OCLS_NM = J2eePolicyMgrImpl.class.getName();
    private static final Logger log = Logger.getLogger(OCLS_NM);
    private static AccessMgr accessMgr;
    private static ReviewMgr reviewMgr;
    private static final String SESSION = "session";

    static
    {
        try
        {
            accessMgr = AccessMgrFactory.createInstance();
            reviewMgr = ReviewMgrFactory.createInstance();
            log.info(J2eePolicyMgrImpl.class.getName() + " - Initialized successfully");
        }
        catch (SecurityException se)
        {
            String error = OCLS_NM + " caught SecurityException=" + se;
            log.fatal(error);
        }
    }


    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return boolean true if succeeds, false otherwise.
     * @throws com.jts.fortress.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public boolean authenticate(String userId, String password)
        throws SecurityException
    {
        boolean result = false;
        Session session = accessMgr.authenticate(userId, password);
        if (session != null)
        {
            result = true;
            if (log.isEnabledFor(Level.DEBUG))
            {
                log.debug(OCLS_NM + ".authenticate userId <" + userId + "> successful");
            }
        }
        else
        {
            if (log.isEnabledFor(Level.DEBUG))
            {
                log.debug(OCLS_NM + ".authenticate userId <" + userId + "> failed");
            }
        }

        return result;
    }


    /**
     * Perform user authentication {@link User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link com.jts.fortress.rbac.Session} that contains target user's RBAC {@link User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link com.jts.fortress.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link com.jts.fortress.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>, see {@link com.jts.fortress.pwpolicy.openldap.OLPWControlImpl#checkPasswordPolicy(com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPConnection, boolean, com.jts.fortress.pwpolicy.PwMessage)}.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link com.jts.fortress.rbac.User#isLocked()}, regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link com.jts.fortress.util.time.Constraint}(s) on {@link User}, {@link com.jts.fortress.rbac.UserRole} and {@link com.jts.fortress.arbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link com.jts.fortress.rbac.DSD#validate(com.jts.fortress.rbac.Session, com.jts.fortress.util.time.Constraint, com.jts.fortress.util.time.Time)} on {@link com.jts.fortress.rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link com.jts.fortress.rbac.Session} containing {@link com.jts.fortress.rbac.Session#getUser()}, {@link com.jts.fortress.rbac.Session#getRoles()} and {@link com.jts.fortress.rbac.Session#getAdminRoles()} if everything checks out good.
     * <li> throw a checked exception that will be {@link com.jts.fortress.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link com.jts.fortress.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link com.jts.fortress.ValidationException} for data validation errors.
     * <li> throw a {@link com.jts.fortress.FinderException} if User id not found.
     * </ul>
     * <h4>
     * The function is valid if and only if:
     * </h4>
     * <ul>
     * <li> the user is a member of the USERS data set
     * <li> the password is supplied (unless trusted).
     * <li> the (optional) active role set is a subset of the roles authorized for that user.
     * </ul>
     * <h4>
     * The following attributes may be set when calling this method
     * </h4>
     * <ul>
     * <li> {@link User#userId} - required
     * <li> {@link com.jts.fortress.rbac.User#password}
     * <li> {@link com.jts.fortress.rbac.User#roles} contains a list of RBAC role names authorized for user and targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this Session.
     * <li> {@link com.jts.fortress.rbac.User#adminRoles} contains a list of Admin role names authorized for user and targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
     * <li> {@link User#props} collection of name value pairs collected on behalf of User during signon.  For example hostname:myservername or ip:192.168.1.99
     * </ul>
     * <h4>
     * Notes:
     * </h4>
     * <ul>
     * <li> roles that violate Dynamic Separation of Duty Relationships will not be activated into session.
     * <li> role activations will proceed in same order as supplied to User entity setter, see {@link User#setRole(String)}.
     * </ul>
     * </p>
     *
     * @param userId   maps to {@link com.jts.fortress.rbac.User#userId}.
     * @param password maps to {@link com.jts.fortress.rbac.User#password}.
     * @return TcPrincipal which contains the User's RBAC Session data formatted into a java.security.Principal that is used by Tomcat runtime.
     * @throws com.jts.fortress.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public TcPrincipal createSession(String userId, String password)
        throws SecurityException
    {
        Session session = accessMgr.createSession(new User(userId, password), false);
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".createSession userId <" + userId + "> successful");
        }
        HashMap<String, Session> context = new HashMap<String, Session>();
        context.put(SESSION, session);
        return new password)
 );
      on                                                                                                                  * T                  maps to {@link c policy violations.
     * <li> thry user who not be acvern{@cod    T Duty }ng u'    '), ot be acvon} if User id not, ot be acvon} if   in the event of data validation fail                 T Duty rtress.at violate Dynsionjts.fortom.jts.fortress.rrExceptiobjpasurn nere, secof Admin role naluate tcod  ress.rbac.User#password}
     * <li> is Sek c pich contaid for that user.
  Id} - required
     * <li> {@link com.jts,y violaRntaid for that user.
  Id} - required
     * <li> {@linkink com.jts.f,m.jts.fo pwer RBAC Sod  user.
  Id} - required
     * <li> {@lwar metk c policy violations.
     * <li> * <li> ixpi contebe onds User#userId} - required
     * <li> {@liraceLog   s.fortmorion to checking user password validity it will apply configured password policy checks {@link com.jts.fortress.rbac.User#pwPolicy}..<br />
     * Method may also store prExceptiicate user pas     @lines()} if    T Duty ortEntity}.
     * <h4> This API will...</h4>
   rm <a hrefDtp:/="http:(org/">OpenLDAP</a> <a href="http://tools.ietf.org/html/draft-behera-ldap-pass.i> {@liId      > assword policy evaluatThe foll> authenticate user pas@lines  T Duty o, com.jts.fortress.pwpoliDcPrrk c     ged laRntai as        d.securitet user   * Inms passed objpas     nk com.jtsdoeynsion        hi * <li> r polac.S        on target user;
        R activrolaoad   conteer#userpjdk.LDAPCo    Cboolea(                 )}                      ps passed          et user   * In's RBAC {@link UseRoles}nclud  u;
        R actng other methodsonta      Mis class.
     * The successful resuR ac# {
 com.jts.fortress.rb       Rntai ason bewordpjdk.LDAPCo                                                                                     s {@link com.jts.fortre is Session * Met                             has     ms passed pk.LDAPCo  .jts.foonta    ortEntity}.
     * <h4> This API will...</h4>
    jts.fo    f Duti                  .has    "<li> perform <a hrefDtp:/="http:(org/">OpenLDAP</a> <a href="http://    f Duti         behera-ldap-pk.LDAPCo              sion);
ldap-onta        sassword policyword poli// Fd exclosrtress.r                           word poli// ms passed .fortre, secofwpolicy.e.Constraint}(sa          sExceptiobjpas.y evaluation</a>, see {@link com.jts.fortres((pjdk.LDAPCo)-pk.LDAPCo)    Cboolea(ssword poli     .ess.rtd <N   (ts.fort,             .icy(_CTXT_NULL,o    f Duti    )  word poli//   nk cy.e.fortre, secofw         ink com:</h4>
     * <ul>
     * <lkPasswor   licy(comssword poli     .ess.rtd <N   (s the use            .  * _icy(_NULL,o    f Duti    )  word poli// C</ul>     @link comcata set
 sword polink com:</h4>
        . * </h4>data set
 s(s the use     .data set
 Type.  * 
     * <li> perfo// C</ul>R acti@link comcata set
 s;sdon't* </ul>
SD:</h4>
        . * </h4>data set
 s(s the use     .data set
 Type.ROLE
     * <li> perfo// Gle( pol * <li>d, String p      fromd polink com:</h4>
   Set, see {.Time)ZR acti=ext.put(Sinki, String om.jts.<li> perform <ime)ZR acti        &&Time)ZR act.sing   > 0rg/">OpenLDAP</a> <a hre// Does( pol * <li>d, String p      re, secofw {
  m           polonetress.rbac?AP</a> <a hrerm <ime)ZR act.traint}((onta    o                                // Yes, wvela of  m    .                                                                                          f="http://    f Duti         behera-ldap-pk.LDAPCo              sion);
ldap-onta        s                               }                                            }                                                                                                                         // UNotes:
siond, String pord poiron
    {
.                     f="http://    f Duti         behera-ldap-pk.LDAPCo              sis:
siond, String pon);
ldap-onta        sassword poli        }                                                            // UNotedoeynsionla of nyomcat runtime     ord poiron
    {
.                          f Duti         behera-ldap-pk.LDAPCo              sih    o>d, String p     assword policy evaluatThe fol                               f Dutiecatds Rntai       fromd polon);
c         orddfor                            onta     his class.
    R ac# {
 c,clasbeecatdom.jts.fortress.rRntai       .Constrr   ponds.secuation or D                                                                         thr     n    tion oed on be is Session * Me oc   s                     Rntaicatd           oonta    ortEntity}.
     * <h4> This API will...</h4>
   The fol         .catd     .lda     onta    o                          SearchncipaR activslog.      ged laUin same ord               search      oMis class.
     * The successful resuior to calling other methodslimit/h4>
   crtresss( pol ing<li>ac.S         * <The fortom.jts.fortress.rL policytype  jts.fog this meth pols.
     * The successful resuR ac# {
 c<li>derIdslog.   R actng other mking user password validity it will apply configured password policy checks {@link com.jts.fortre />
     * Method may also store pL po, see {.Tsearchom.jts jts.fosearch      , conslimitortEntity}.
     * <h4> This API will...</h4>
   The fol         .fin om.jtssearch      , limito                         f Dutiecae fos m    s.fo            Roles}.<        d.securitthat eop);
c         ordthatdfor                            behera-his class.
     to {@link coRolesm     s     rd ordthatdfor        behera-}.<g     ly uniq    y configured passwwwwww eop);
c        om.jts.fortress.r       g this methm    s.fo * T s {@ng other mking us                     t   rd oed on be is Session * Me oc   s                          catd{@linrail {@link cortEntity}.
     * <h4> This API will...</h4>
   The fol         .catd{@linsword if trusteo                          Rhe follol policytype  jts.foli>derItrussword pol eop);
c         Rolesm        * Notra-fieldtress.rbac.            on API.
  nk com.jts}.<br />
     *                               sm x    * </h4>The fortItrusswer#setRole(
     *cone     imit/argsame ord               search      otraint}(saerIis So      dc.Userars .Constrr   pond       rs com.jd ordthatdfor      g other methodslimit/h4>
   cone    ew Useoles( polm x The fortIt   rdsom.jts.fortress.rL policytype  jts.fog this methm    s.fo * TIdtng other mking us                   rd policy checkSession * Method may also store pL po, see {.Tsearch    ts jts.fosearch      , conslimitortEntity}.
     * <h4> This API will...</h4>
   The fol         .fin     tssword if search      ), limito                           nk le hostnacae fos  pol * <li>   rs vslog.      a ged laonta.example hostname:myservername        or ip:1d polon);
s:
     * </h4>
   ROLE<ul>
      on API.
    m x    * </h4>   rs The fortI}.<   a set
 />
   imit/argu     on API.
  nk com.jts}.<br />
     *                              nk com.jtsdoeynNOT     hierarchical  resu                      onta     his class.
    R ac# {
 cl whintai       vslog.       in same ordemethodslimit/h4>cone    ew Useoles( polm x The fortIt   rdsom.jts.fortress.rL policytype  jts.fog this meth * TIdt vslog.      a ethticularaonta.g other mking user password validity it will apply configured password policy checks {@link com.jtsis Session * Method may also store pL po, see {.Tvslog.      ts jts.foonta    , conslimitortEntity}.
     * <h4> This API will...</h4>
   The fol         .vslog.      ts.lda     onta    o, limito                           nk le hostnacae fos  pol * <li>e activations will pra ged la in sexample hostname:myserver        ame or ip:1    * Notes:
     * </h4>
     * <ul>
                            behera-his class.
     to {@link com    s.fo            com.jd ordthatdfor      g other mtress.rrEolicytype  jts.fog this meth polr activslog.   is lockeswor   itrtom.jts.forking us                  I4>   r oed on be is Session * Me oc   s                     L po, see {.Tv, String om.jtsrail {@link cortEntity}.
     * <h4> This API will...</h4>
   L po, see {.Tl pol      <li> perfo//   nk rn ner</ul>@link comcata set
 swoc.     is lR actng othe     * <ul>
     * <lkcate user password if trusteo,      <li> perfo// Gle( polS* <li>d, String pR actng othe     t, see {.Time)ZR acS* <=             Ir   itrtom.jtsse.put(Sinkom.jts. <li> perfo// I;
     h   d, String p     ng othe   rm <ime)ZR acS* <        &&Time)ZR acS* .sing   > 0rg/">OpenLDAP</a> <a hre// Convere( polS* <nd AdmiL polbefo     ess.e {:AP</a> <a hrel pol   wor         , see {.<ime)ZR acS* ssword policy evaluatThe foll     policy   